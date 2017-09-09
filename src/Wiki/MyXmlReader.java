package Wiki;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;
import java.util.regex.Pattern;


class ReaderThread implements Runnable {
    private long startOffset = 0;
    private long endOffset = 0;
    private String currTag = "";
    private char[] text;
    private int tid;
    private Integer docId;
    Pattern regexPattern, extraPattern;
    ArrayList<Pair<String, String>> docToOffset;

    ReaderThread(long start, long end, int tid) {
        this.startOffset = start;
        this.endOffset = end;
        this.tid = tid;
        this.regexPattern = Pattern.compile("[^a-zA-Z0-9\\-\']+");
        this.extraPattern = Pattern.compile("-+");
        this.docToOffset = new ArrayList<>();
        this.docId = 0;
    }

    private void parse(XMLStreamReader xmlr) throws Exception {
        long bytesRead = 0;
        String elementName = "";
        while (xmlr.hasNext()) {
            int eventType = xmlr.next();
            switch (eventType) {
                case XMLEvent.START_ELEMENT:
                    elementName = xmlr.getLocalName();
                    if (elementName.equals("page")) {
                        String relDocId = Integer.toString(this.tid) + "_" + this.docId;
                        this.docToOffset.add(new Pair<String, String>(relDocId,
                                Integer.toString(xmlr.getLocation().getCharacterOffset())
                        ));
                        this.docId += 1;
                        readPageTag(xmlr);
                    }
                    break;
                case XMLEvent.END_ELEMENT:
                    break;
            }
            bytesRead = xmlr.getLocation().getCharacterOffset();
            if (bytesRead>endOffset) return;
        }
    }

    private void readPageTag(XMLStreamReader xmlr) throws Exception {
        String elementName = "";
        String insideText = "";
        while (xmlr.hasNext()) {
            int eventType = xmlr.next();
            switch (eventType) {
                case XMLStreamConstants.START_ELEMENT:
                    elementName = xmlr.getLocalName();
                    if (Arrays.asList(GlobalVars.mainTags).contains(elementName)) currTag = elementName;
                    break;
                case XMLStreamConstants.CHARACTERS:
                    insideText = xmlr.getText();
                    System.out.println("Text is :" + insideText);
                    String[] words = insideText.split(" ");
                    for (String word : words) {
                        if (!word.isEmpty()) GlobalVars.readerParserBuffer[this.tid].add(new Tuple<>(this.docId, word, currTag));
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    elementName = xmlr.getLocalName();
                    currTag = "general_docs";
                    if (elementName.equals("page")) return;
                    break;
            }
        }
    }

    @Override
    public void run() {
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            InputStream fStream = new FileInputStream(GlobalVars.xmlFileName);
            BufferedReader fReader = new BufferedReader(new InputStreamReader(fStream, "utf-8"));
            fReader.skip(startOffset);
//            XMLStreamReader xmlStream = factory.createXMLStreamReader(new FileReader(GlobalVars.xmlFileName));
            XMLStreamReader xmlStream = factory.createXMLStreamReader(fReader);
            parse(xmlStream);
        } catch (Exception e) {
            System.err.println("Error occurred while parsing :(");
            e.printStackTrace();
        }
    }
}

public class MyXmlReader {
    public long fileSize = 0;
    public String xmlFileName;
    MyXmlReader(String fileName, long fileSize) {
        this.xmlFileName = fileName;
        this.fileSize = fileSize;
    }
    public void start() {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(GlobalVars.numOfReaderThreads);
        long startOffset = 0;
        long lengthToRead = fileSize/GlobalVars.numOfReaderThreads;
        for (int i = 0; i < GlobalVars.numOfReaderThreads; i++) {
            ReaderThread reader = new ReaderThread(startOffset, startOffset+lengthToRead, i);
            executor.execute(reader);
        }
        executor.shutdown();
    }
}
