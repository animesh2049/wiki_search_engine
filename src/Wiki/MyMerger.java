package Wiki;

import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class StringComparator implements Comparator<Tuple<String, String, BufferedReader>> {

    public int compare(Tuple<String, String, BufferedReader> a, Tuple<String, String, BufferedReader> b) {
        return a.getFirst().compareTo(b.getFirst());
    }
}

class MergerThread implements Runnable {
    private Integer tid;
    private String outputFolderPath;
    private final Lock lock;
    private final Condition cond;
    private int localFileCounter;
    private PriorityQueue<Tuple<String, String, BufferedReader> > priorityQueue;
    private int filesEnded;
    private ArrayList<BufferedReader> filesCompletelyRead = new ArrayList<>();


    MergerThread(Integer tid) {
        this.tid = tid;
        this.outputFolderPath = GlobalVars.tempOutputFolderPath + "/";
        this.lock = new ReentrantLock();
        this.cond = this.lock.newCondition();
        this.localFileCounter = 0;
        this.priorityQueue = new PriorityQueue<>(2*GlobalVars.mergeFactor, new StringComparator());
        this.filesEnded = 0;
    }

    public Integer getTid() {
        return this.tid;
    }

    public Condition getCond() {
        return this.cond;
    }

    public Lock getLock() {
        return this.lock;
    }

    private void mergeFiles(ArrayList<File> fileList) throws Exception {
        String fileNameToWrite = this.outputFolderPath + this.tid + "-" + this.localFileCounter;
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileNameToWrite));
        System.out.println("Came to merge");

        ArrayList<BufferedReader> filesToMerge = new ArrayList<>();
        for (File file : fileList) filesToMerge.add(
                new BufferedReader(new InputStreamReader(new FileInputStream(file)))
        );

        int totalNumOfFiles = filesToMerge.size();
        for (int i=0; i<totalNumOfFiles; i++) {
            String firstLine = filesToMerge.get(i).readLine();
            if (firstLine == null) this.filesEnded += 1;
            else {
                String[] wordPosting = firstLine.split("=");
                if (wordPosting.length < 2) {
                    continue;
                }
                this.priorityQueue.add(new Tuple<>(wordPosting[0], wordPosting[1], filesToMerge.get(i)));
            }
        }

        Tuple<String, String, BufferedReader> prevWord;
        Tuple<String, String, BufferedReader> newWord;
        ArrayList<String> postings = new ArrayList<>();
        while (true) {
            prevWord = this.priorityQueue.poll();
            if (prevWord != null) {
                addNewLine(prevWord.getThird());
//                System.out.println("word " + prevWord.getFirst());
                postings.add(prevWord.getSecond());
                newWord = this.priorityQueue.peek();
                while ((prevWord != null) && (newWord != null) && (prevWord.getFirst().equals(newWord.getFirst()))) {
                    newWord = this.priorityQueue.poll();
                    postings.add(newWord.getSecond());
                    addNewLine(newWord.getThird());
                    newWord = this.priorityQueue.peek();
                    int temp;
                    temp = 4;
                }
                flushToFile(prevWord.getFirst(), postings, writer);
                postings.clear();
                filesToMerge.removeAll(this.filesCompletelyRead);
                this.filesCompletelyRead.clear();
            }
            if (this.filesEnded == totalNumOfFiles) break;
        }
        writer.close();
        Task myTask = new Task(fileNameToWrite, new File(fileNameToWrite).length(), false);
        GlobalVars.taskQueue.add(myTask);
        System.out.println("done merging");
        for (File file : fileList) {
            file.delete();
        }
        this.localFileCounter += 1;
    }

    private void addNewLine(BufferedReader fReader) throws Exception {
        String newLineRead;
        newLineRead = fReader.readLine();
        if (newLineRead == null) {
            this.filesEnded += 1;
            this.filesCompletelyRead.add(fReader);
        }
        else {

            String[] line = newLineRead.split("=");
            if (line.length < 2) return;
            if (line.length == 2)
                this.priorityQueue.add(new Tuple<>(line[0], line[1], fReader));
            else
                this.priorityQueue.add(new Tuple<>(line[0] + line[1], line[2], fReader));
        }
    }

    private void flushToFile(String word, ArrayList<String> postings, BufferedWriter writer) throws Exception {
        Posting mergedFinalPosting = unMarshalPostings(postings);
        TreeMap<String, Posting> myMap = new TreeMap<>();
        myMap.put(word, mergedFinalPosting);
        String toWrite = marshalPosting(myMap);
        writer.write(toWrite);
    }

    private Posting unMarshalPostings(ArrayList<String> postings) {
        TreeMap<Integer, TreeMap<String, Integer>> postingMap = new TreeMap<>();
        for (String posting : postings) {
            String[] docDetails = posting.split(";");
            for (String docDetail : docDetails) {
                String[] docIdAndTags = docDetail.split(" ");
                int numOfTags = docIdAndTags.length;
                if (numOfTags < 2) continue;
                Integer docId = Integer.valueOf(docIdAndTags[0]);
                postingMap.put(docId, new TreeMap<>());
                String[] tagsFreqs = docIdAndTags[1].split(",");
                for (String tag : tagsFreqs) {
                    String[] tagFreq = tag.split(":");
                    postingMap.get(docId).put(tagFreq[0], Integer.valueOf(tagFreq[1]));
                }
            }
        }
        return new Posting(postingMap);
    }

    private String marshalPosting(TreeMap<String, Posting> newPosting) {
        StringBuilder toWrite = new StringBuilder("");
        for (Map.Entry<String, Posting> stringPosting : newPosting.entrySet()) {
            toWrite.append(stringPosting.getKey()).append("=");
            for (Map.Entry<Integer, TreeMap<String, Integer>> docToListEntry : stringPosting.getValue().docToFreq.entrySet()) {
                toWrite.append(docToListEntry.getKey()).append(" ");
                for (Map.Entry<String, Integer> tagToFreqEntry : docToListEntry.getValue().entrySet()) {
                    toWrite.append(tagToFreqEntry.getKey()).append(":").append(tagToFreqEntry.getValue()).append(",");
                }
                toWrite.append(";");
            }
            toWrite.append("\n");
        }
        return toWrite.toString();
    }

    @Override
    public void run() {
        while (true) {
            this.lock.lock();
            Task myTask = new Task(this, true);
            GlobalVars.taskQueue.add(myTask);
            try {
                this.cond.await();
            }
            catch (InterruptedException e) {
                System.out.println("Couldn't sleep on that variable");
                e.printStackTrace();
                continue;
            }
            ArrayList<File> fileList = GlobalVars.fileMergerBuffer[this.tid].poll();
            if (fileList == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            try {
                mergeFiles(fileList);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            this.lock.unlock();
        }
    }
}

public class MyMerger {
    public void start() {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(GlobalVars.numOfMergerThreads);
        for (int i = 0; i < GlobalVars.numOfMergerThreads; i++) {
            MergerThread merger = new MergerThread(i);
            executor.execute(merger);
        }
        executor.shutdown();
    }
}
