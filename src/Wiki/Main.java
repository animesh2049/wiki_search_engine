package Wiki;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Main {
    public static void main(String[] args) {
        /*if (args.length == 0) {
            System.out.println("Usage: main xmlfilename");
            return;
        }*/
//        GlobalVars.xmlFileName = args[0];
        init();
        try {
            initStopWords();
        } catch (Exception e) {
            System.err.println("Error while stop word init");
            e.printStackTrace();
            return;
        }
        File xmlFile = new File(GlobalVars.xmlFileName);
        long fileSize = xmlFile.length();
        MyXmlReader myReader = new MyXmlReader(GlobalVars.xmlFileName, fileSize);
        myReader.start();
        MyWordParser myParser = new MyWordParser();
        myParser.start();
        MyIndexWriter myWriter = new MyIndexWriter();
        myWriter.start();
//        MyMerger myMerger = new MyMerger();
//        myMerger.start();
        ManageMergers myManager = new ManageMergers();
        myManager.start();
    }

    private static void init() {
        for (int i=0; i<GlobalVars.numOfReaderThreads; i++) {
            GlobalVars.readerParserBuffer[i] = new ConcurrentLinkedQueue<>();
            GlobalVars.parserWriterBuffer[i] = new ConcurrentLinkedQueue<>();
        }
        for (int i=0; i<GlobalVars.numOfMergerThreads; i++) {
            GlobalVars.fileMergerBuffer[i] = new ConcurrentLinkedQueue<>();
        }
        GlobalVars.isParsingDone = false;
        GlobalVars.isMergingDone = false;
      //  GlobalVars.taskQueue = new ConcurrentLinkedQueue<>();
    }

    private static void initStopWords() throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(GlobalVars.stopWordFile));
        String line = br.readLine();
        while (line != null) {
            GlobalVars.stopWords.put(line, true);
            line = br.readLine();
        }
    }
}
