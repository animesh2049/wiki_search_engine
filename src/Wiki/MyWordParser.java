package Wiki;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

//class Tuple {
////    public int docId;
////    public int[] fieldFrequency;
//    HashMap<Integer, int[]> docToFreq;
//
//    Tuple(int docId) {
//        this.docId = docId;
//        this.fieldFrequency = new int[GlobalVars.numOfReaderThreads];
//    }
//}

class Posting {
    HashMap<Integer, int[]> docToFreq;
    public int totalFrequency;

    Posting() {
        this.docToFreq = new HashMap<>();
        this.totalFrequency = 0;
    }

    void pushBack(int[] fieldFrequency, int docId) {

        for (int i : fieldFrequency) this.totalFrequency += i;
    }
}

class ParserThread implements Runnable {
    private HashMap<String, Posting> postingList;
    int tid;

    ParserThread(int tid) {
        postingList = new HashMap<>();
        this.tid = tid;
    }

    @Override
    public void run() {
        while (true) {
            Tuple<Integer, String, String> parseTask = GlobalVars.readerParserBuffer[this.tid].poll();
            if (parseTask != null) {
                int index;
                for (index = 0; index < GlobalVars.mainTags.length; index++) {
                    if (GlobalVars.mainTags[index] == parseTask.getThird()) break;
                }
                String tempWord = parseTask.getSecond().toLowerCase();
            }
        }
    }
}

public class MyWordParser {
    public void start() {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(GlobalVars.numOfReaderThreads);
        for (int i=0; i<GlobalVars.numOfParserThreads; i++) {
            ParserThread parser = new ParserThread(i);
            executor.execute(parser);
        }
    }
}
