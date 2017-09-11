package Wiki;


import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class ReaderThread implements Runnable {
    private long startOffset = 0;
    private long endOffset = 0;
    private String currTag = "";
    private char[] text;
    private int tid;
    private Integer docId;
    private Pattern regexPattern, extraPattern, infoBoxStart, infoBoxEnd, url, category;
    private ArrayList<Pair<String, String>> docToOffset;
    private boolean foundIdTag, foundCategory, foundUrl;
    private boolean foundIdNum, foundInfoBox;
    private long pageOffset;
    private Integer relDocId;
    private ArrayList<Pair<String, String>> tempBuffer;

    ReaderThread(long start, long end, int tid) {
        this.startOffset = start;
        this.endOffset = end;
        this.tid = tid;
        this.regexPattern = Pattern.compile("[^a-zA-Z0-9\\-\']+");
        this.extraPattern = Pattern.compile("-{2,}|'{2,}");
        this.infoBoxStart = Pattern.compile("\\{\\{Infobox .*");
        this.infoBoxEnd = Pattern.compile(".*\n}}\n.*|^}}\n.*", Pattern.DOTALL);
        this.url = Pattern.compile("http://[^ ]*");
        this.category = Pattern.compile("\\[\\[Category:.*]]");
        this.docToOffset = new ArrayList<>();  // Use at the time of querying; Maps docId to the offset where page tag starts
        this.docId = 0;
        this.foundIdTag = false;
        this.foundIdNum = false;
        this.tempBuffer = new ArrayList<>();
        this.relDocId = 0;
        this.foundCategory = false;
        this.foundInfoBox = false;
        this.foundUrl = false;

        /*int charsRead;
        List<String> tempChars = new ArrayList<>();
        System.out.println("Initial start offset :" + startOffset);
        try {
            FileReader tempReader= new FileReader(GlobalVars.xmlFileName);
            tempReader.skip(this.startOffset);
            while ((charsRead = tempReader.read()) != -1) {
                tempChars.add(String.valueOf((char)charsRead));
                this.startOffset += 1;
                if (tempChars.size() > 5) tempChars.remove(0);
                String temp = "";
                for (String i : tempChars) temp += i;
                if (temp.equals("<page")) break;
            }
            this.startOffset -= 5;
            System.out.println("Final offset is : " + startOffset);
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    private void parse(XMLStreamReader xmlr) throws Exception {
        long bytesRead = 0;
        String elementName;
        while (xmlr.hasNext()) {
            int eventType = xmlr.next();
            switch (eventType) {
                case XMLEvent.START_ELEMENT:
                    elementName = xmlr.getLocalName();
                    if (elementName.equals("page")) {
                        this.relDocId += 1;
                        this.pageOffset = xmlr.getLocation().getCharacterOffset();
                        readPageTag(xmlr);
                    }
                    break;
                case XMLEvent.END_ELEMENT:
                    break;
            }
            bytesRead = xmlr.getLocation().getCharacterOffset();
            if (bytesRead>=endOffset) {
                System.out.println("End of reading from xmlreader :)");
                GlobalVars.readerParserBuffer[this.tid].add(new Tuple<>(this.docId, "^$", ""));
                return;
            }
        }
        GlobalVars.readerParserBuffer[this.tid].add(new Tuple<>(this.docId, "^$", ""));
        return;
    }

    private void readPageTag(XMLStreamReader xmlr) throws Exception {
        String elementName;
        String insideText;
        while (xmlr.hasNext()) {
            int eventType = xmlr.next();
            switch (eventType) {
                case XMLStreamConstants.START_ELEMENT:
                    elementName = xmlr.getLocalName();
                    if (Arrays.asList(GlobalVars.mainTags).contains(elementName)) this.currTag = elementName;
                    else if (elementName.equals("id") && !this.foundIdTag) this.foundIdTag = true;
                    break;
                case XMLStreamConstants.CHARACTERS:
                    insideText = xmlr.getText();
                    if (this.foundIdTag && !this.foundIdNum) {
                        this.docId = Integer.valueOf(insideText);// This add extra things i.e. It treads Id as
                        this.foundIdNum = true;                  // different field than others. So this won't appear in indices
                        for (Pair<String, String> temp : tempBuffer) {
                            GlobalVars.readerParserBuffer[this.tid].add(new Tuple<>(this.docId, temp.getFirst(), temp.getSecond()));
                        }
                        this.tempBuffer.clear(); // Check if it deletes the contents of arraylist becuase next add should start from the begining
                        this.docToOffset.add(new Pair<>(insideText, String.valueOf(this.pageOffset)));
                        continue;
                    }
                    /*
                        Here we can filter things like id of revision, user in
                        order to reduce the index size.
                     */
                    Matcher urlMatcher = this.url.matcher(insideText);
                    while (urlMatcher.find()) {
                        this.foundUrl = true;
                        GlobalVars.readerParserBuffer[this.tid].add(
                                new Tuple<>(this.docId, urlMatcher.group(), "external_links")
                        );
                    }
                    if (this.foundUrl) {
                        this.foundUrl = false;
                        continue;
                    }
                    Matcher categoryMatcher = this.category.matcher(insideText);
                    while (categoryMatcher.find()){
                        this.foundCategory = true;
                        GlobalVars.readerParserBuffer[this.tid].add(
                                new Tuple<>(this.docId, categoryMatcher.group().substring(11), "category")
                        );
                    }
                    if (this.foundCategory) {
                        this.foundCategory = false;
                        continue;
                    }
                    Matcher infoboxMatcher = infoBoxStart.matcher(insideText);
                    if (infoboxMatcher.find()) {
                        this.foundInfoBox = true;
                        this.currTag = "infobox";
                    }
                    Matcher infoBoxEndMatcher = infoBoxEnd.matcher(insideText);
                    if (this.foundInfoBox && infoBoxEndMatcher.find()) {
                        this.foundInfoBox = false;
                        this.currTag = "text";
                    }
                    insideText = regexPattern.matcher(insideText).replaceAll(" ");
                    insideText = extraPattern.matcher(insideText).replaceAll("");
                    String[] words = insideText.split(" ");
                    for (String word : words) {
                        if (!word.isEmpty()) {
                            if (this.foundIdNum) {
                                GlobalVars.readerParserBuffer[this.tid].add(new Tuple<>(this.docId, word, this.currTag));
                            }
                            else  {
                                this.tempBuffer.add(new Pair<>(word, this.currTag));
                            }
                        }
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    elementName = xmlr.getLocalName();
                    this.currTag = "general_docs";
                    if (elementName.equals("page")) {
                        if ((this.relDocId % GlobalVars.flushFactor) == 0) {
                            GlobalVars.readerParserBuffer[this.tid].add(new Tuple<>(this.docId, "$$", ""));
                        }
                        this.foundIdTag = false;
                        this.foundIdNum = false;
                        return;
                    }
                    break;
            }
        }
    }

    @Override
    public void run() {
        try {
            InputStream fStream = new FileInputStream(GlobalVars.xmlFileName);
            BufferedReader fReader = new BufferedReader(new InputStreamReader(fStream, "UTF-8"));
            fReader.skip(startOffset);
            XMLInputFactory factory = XMLInputFactory.newInstance();
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
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        long startOffset = 0;
        long lengthToRead = fileSize/GlobalVars.numOfReaderThreads;
        for (int i = 0; i < GlobalVars.numOfReaderThreads; i++) {
            ReaderThread reader = new ReaderThread(startOffset, startOffset+lengthToRead, i);
            startOffset += lengthToRead + 1;
            executor.execute(reader);
        }
        executor.shutdown();
    }
}
