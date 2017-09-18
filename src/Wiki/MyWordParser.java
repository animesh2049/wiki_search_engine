package Wiki;

import sun.reflect.generics.tree.Tree;

import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


class Posting {
    public TreeMap<Integer, TreeMap<String, Integer>> docToFreq;

    Posting() {
        this.docToFreq = new TreeMap<>();
    }

    Posting(TreeMap<Integer, TreeMap<String, Integer>> posting) {
        this.docToFreq = posting;
    }
}

class ParserThread implements Runnable {
    private TreeMap<String, Posting> postingList;
    private int tid;

    ParserThread(int tid) {
        postingList = new TreeMap<>();
        this.tid = tid;
    }

    private void putWord(String word, String tagName, Integer docId) {
        Posting temporaryPosting = this.postingList.get(word);
        if (temporaryPosting != null) {
            TreeMap<String, Integer> docFreq = temporaryPosting.docToFreq.get(docId);
            if (docFreq != null) {
                docFreq.merge(tagName, 1, Integer::sum);
            }
            else {
                TreeMap<String, Integer> tempEntry = new TreeMap<>();
                tempEntry.put(tagName, 1);
                this.postingList.get(word).docToFreq.put(docId, tempEntry);
            }
        }
        else {
            Posting tempPosting = new Posting();
            TreeMap<String, Integer> tempEntry = new TreeMap<>();
            tempEntry.put(tagName, 1);
            tempPosting.docToFreq.put(docId, tempEntry);
            this.postingList.put(word, tempPosting);
        }
    }

    private void flush() {
        TreeMap<String, Posting> tempMap = new TreeMap<>(this.postingList);
        GlobalVars.parserWriterBuffer[this.tid].add(tempMap);
        this.postingList.clear();
    }

    @Override
    public void run() {
        while (true) {
            Tuple<Integer, String, String> parseTask = GlobalVars.readerParserBuffer[this.tid].poll();
            if (parseTask != null) {
                String tempWord = parseTask.getSecond().toLowerCase();
                if (tempWord.equals("^$")) {
                    System.out.println("End of parsing");
                    TreeMap<String, Posting> endSignal = new TreeMap<>();
                    endSignal.put("^end$", new Posting());
                    GlobalVars.parserWriterBuffer[this.tid].add(endSignal);
                    return;
                }
                if (tempWord.equals("$$")) {
                    flush();
                    continue;
                }
                tempWord = GlobalVars.myStemmer.add(tempWord);
                if (!tempWord.equals(""))
                    putWord(tempWord, parseTask.getThird(), parseTask.getFirst());
            }
        }
    }
}

public class MyWordParser {
    public void start() {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        for (int i=0; i<GlobalVars.numOfParserThreads; i++) {
            ParserThread parser = new ParserThread(i);
            executor.execute(parser);
        }
        executor.shutdown();
    }
}
