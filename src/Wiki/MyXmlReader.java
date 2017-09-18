package Wiki;


import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
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
    private Pattern allregex, regexPattern, extraPattern, infoBoxStart, infoBoxEnd, url, category;
    private Pattern newExtraPattern;
    private ArrayList<Pair<String, String>> docToOffset;
    private boolean foundIdTag, foundCategory, foundUrl;
    private boolean foundIdNum, foundInfoBox;
    private long pageOffset;
    private int infoBoxStartposition, infoBoxEndposition;
    private Integer relDocId;
    private ArrayList<Pair<String, String>> tempBuffer;

    ReaderThread(long start, long end, int tid) {
        this.startOffset = start;
        this.endOffset = end;
        this.tid = tid;
        this.regexPattern = Pattern.compile("[^a-zA-Z0-9\\-\']+");
        this.extraPattern = Pattern.compile("([a-zA-Z0-9]+[\\-\']?) *[a-zA-Z0-9]+");
        this.infoBoxStart = Pattern.compile("\\{\\{Infobox");
        this.infoBoxEnd = Pattern.compile("\n}}\n", Pattern.DOTALL);
        this.newExtraPattern = Pattern.compile("([a-zA-Z0-9]+[\\-]?)*[a-zA-Z0-9]+");
        this.url = Pattern.compile("http://[a-zA-Z0-9.:/?&='\"]*");
        this.category = Pattern.compile("\\[\\[Category:.*]]");
        this.allregex = Pattern.compile("http://[^ ]*|\\[\\[Category:.*]]|\\{\\{Infobox .*|\n}}\n", Pattern.DOTALL);
        this.docToOffset = new ArrayList<>();  // Use at the time of querying; Maps docId to the offset where page tag starts
        this.docId = 0;
        this.foundIdTag = false;
        this.foundIdNum = false;
        this.tempBuffer = new ArrayList<>();
        this.relDocId = 0;
        this.foundCategory = false;
        this.foundInfoBox = false;
        this.foundUrl = false;
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
            if (bytesRead >= endOffset) {
                System.out.println("End of reading from xmlreader :)");
                GlobalVars.readerParserBuffer[this.tid].add(new Tuple<>(this.docId, "^$", ""));
                return;
            }
        }
        GlobalVars.readerParserBuffer[this.tid].add(new Tuple<>(this.docId, "^$", ""));
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
                        this.docId = Integer.valueOf(insideText);
                        this.foundIdNum = true;
                        for (Pair<String, String> temp : tempBuffer) {
                            GlobalVars.readerParserBuffer[this.tid].add(new Tuple<>(this.docId, temp.getFirst(), temp.getSecond()));
                        }
                        this.tempBuffer.clear();
                        continue;
                    }
                    /*
                        Here we can filter things like id of revision, user in
                        order to reduce the index size.
                     */

                    Matcher allMatcher = this.allregex.matcher(insideText);
                    int start = 0, end = 0;
                    while (allMatcher.find()) {
                        end = allMatcher.start();
                        parseExtra(insideText.substring(start, end));
                        start = allMatcher.end();
                        String Found_String = allMatcher.group();
                        Matcher urlMatcher = this.url.matcher(Found_String);
                        Matcher categoryMatcher = this.category.matcher(Found_String);
                        Matcher infoBoxStartMatcher = this.infoBoxStart.matcher(Found_String);
                        if (urlMatcher.find()) {
                            this.foundUrl = true;
                            GlobalVars.readerParserBuffer[this.tid].add(
                                    new Tuple<>(this.docId, urlMatcher.group(), "external_links")
                            );

                        }
                        else if (categoryMatcher.find()) {
                            this.foundCategory = true;
                            String newText = categoryMatcher.group().substring(11);
                            newText = this.regexPattern.matcher(newText).replaceAll(" ");
                            newText = newText.trim();

                            Matcher tempMatcher = this.extraPattern.matcher(newText);
                            if (tempMatcher.find()) newText = tempMatcher.group();
                            else return;
                            String[] catTexts = newText.split(" ");
                            for (String text : catTexts) {
                                if (text.equals("")) continue;
                                GlobalVars.readerParserBuffer[this.tid].add(
                                        new Tuple<>(this.docId, text, "category")
                                );
                            }
                        }
                        else if (infoBoxStartMatcher.find()){
                            this.foundInfoBox = true;
                            this.currTag = "infobox";
                        }
                        else if (this.foundInfoBox) {
                            this.foundInfoBox = false;
                            this.currTag = "text";
                        }
                    }
                    parseExtra(insideText.substring(start));
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

    private void parseExtra(String insideText) {
        insideText = this.regexPattern.matcher(insideText).replaceAll(" ");
        insideText = insideText.trim();

        Matcher tempMatcher = this.extraPattern.matcher(insideText);
        if (tempMatcher.find()) insideText = tempMatcher.group();
        else return;
        String[] words = insideText.split(" ");
        for (String word : words) {
            if (!word.isEmpty()) {
                if (this.foundIdNum) {
                    GlobalVars.readerParserBuffer[this.tid].add(new Tuple<>(this.docId, word, this.currTag));
                } else {
                    this.tempBuffer.add(new Pair<>(word, this.currTag));
                }
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
        System.out.println("Enede reading");
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
        long lengthToRead = fileSize / GlobalVars.numOfReaderThreads;
        for (int i = 0; i < GlobalVars.numOfReaderThreads; i++) {
            ReaderThread reader = new ReaderThread(startOffset, startOffset + lengthToRead, i);
            startOffset += lengthToRead + 1;
            executor.execute(reader);
        }
        executor.shutdown();
    }
}
