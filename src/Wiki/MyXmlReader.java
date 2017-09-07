package Wiki;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.FileReader;
import java.util.Iterator;
import java.util.concurrent.ThreadPoolExecutor;
import javax.xml.stream.*;
import javax.xml.namespace.QName;
import javax.xml.stream.events.XMLEvent;


class ReaderThread implements Runnable {
    private long startOffset = 0;
    private long endOffset = 0;
    private String currTag = "";
    private boolean foundPageTag;
    private char[] text;

    ReaderThread(long start, long end) {
        this.startOffset = start;
        this.endOffset = end;
        this.foundPageTag = false;
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
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    elementName = xmlr.getLocalName();
                    insideText = "";
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
            XMLStreamReader xmlStream = factory.createXMLStreamReader(new FileReader(GlobalVars.xmlFileName));
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
            ReaderThread reader = new ReaderThread(startOffset, startOffset+lengthToRead);
            executor.execute(reader);
        }
        executor.shutdown();
    }
}
