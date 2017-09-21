package Wiki;

import java.io.*;
import java.util.*;


class StringComparator implements Comparator<Tuple<String, String, BufferedReader>> {

    public int compare(Tuple<String, String, BufferedReader> a, Tuple<String, String, BufferedReader> b) {
        return a.getFirst().compareTo(b.getFirst());
    }
}

public class MergerThread extends Thread {
    private ArrayList<File> listFilesToMerge;
    private int tid;
    private int slot;
    private String outputFolderPath;
    private int localFileCounter;
    private int fileDeleted;
    private PriorityQueue<Tuple<String, String, BufferedReader> > priorityQueue;


    MergerThread(int tid,int slot) {
        this.listFilesToMerge = new ArrayList<>();
        this.tid = tid;
        this.slot = slot;
        this.fileDeleted =0;
        this.outputFolderPath = GlobalVars.tempOutputFolderPath + "/";
        this.localFileCounter = 0;
        this.priorityQueue = new PriorityQueue<>(2*GlobalVars.mergeFactor, new StringComparator());
    }

    public int getTid() {
        return this.tid;
    }


    private void mergeFiles(ArrayList<File> fileList) throws Exception {
        int filesEnded = 0;
        String fileNameToWrite = this.outputFolderPath + this.tid + "--" + this.localFileCounter;
        File writeFile = new File(fileNameToWrite);
        BufferedWriter writer = new BufferedWriter(new FileWriter(writeFile));

        ArrayList<BufferedReader> filesToMerge = new ArrayList<>();
        for (File file : fileList) filesToMerge.add(
                new BufferedReader(new InputStreamReader(new FileInputStream(file)))
        );

        int totalNumOfFiles = filesToMerge.size();
        for (int i=0; i<totalNumOfFiles; i++) {
            filesEnded += addNewLine(filesToMerge.get(i));
        }

        Tuple<String, String, BufferedReader> prevWord;
        Tuple<String, String, BufferedReader> newWord;
        ArrayList<String> postings = new ArrayList<>();
        while (true) {
            prevWord = this.priorityQueue.poll();
            if (prevWord != null) {
                filesEnded += addNewLine(prevWord.getThird());
                postings.add(prevWord.getSecond());
                newWord = this.priorityQueue.peek();
                while ((newWord != null) && (prevWord.getFirst().equals(newWord.getFirst()))) {
                    newWord = this.priorityQueue.poll();
                    postings.add(newWord.getSecond());
                    filesEnded += addNewLine(newWord.getThird());
                    newWord = this.priorityQueue.peek();
                }
                flushToFile(prevWord.getFirst(), postings, writer);
                postings.clear();
            }
            if (filesEnded == totalNumOfFiles) break;
        }
        writer.close();
        GlobalVars.fileMergerBuffer.add(writeFile);
        for (File oldFiles : fileList) {
            oldFiles.delete();
        }
        this.localFileCounter += 1;
    }

    private int addNewLine(BufferedReader fReader) throws Exception { ;
        while(true){
            String newLineRead = fReader.readLine();
            if(newLineRead == null ) return 1;
            String[] line = newLineRead.split("=");
            if (line.length < 2) continue;
            if (line.length == 2)
                this.priorityQueue.add(new Tuple<>(line[0], line[1], fReader));
            else
                this.priorityQueue.add(new Tuple<>(line[0] + line[1], line[2], fReader));
            break;
        }
        return 0;
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
        Boolean keepRunning = true;
        while (keepRunning) {
            File file;
            listFilesToMerge.clear();
            while ((file = GlobalVars.fileMergerBuffer.poll()) != null) {
                listFilesToMerge.add(file);
            }
            System.out.println("thread " + this.tid + " got: " + listFilesToMerge.size() + " files");
            if (listFilesToMerge.size() == 0) break;
            this.fileDeleted += listFilesToMerge.size() - 1;
            if (listFilesToMerge.size() == 1) {
                GlobalVars.fileMergerBuffer.add(listFilesToMerge.get(0));
                break;
            }
            if (listFilesToMerge.size() < GlobalVars.estimatedFilesToMerge) keepRunning = false;
            try {
                mergeFiles(listFilesToMerge);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("Merge Done by thread"+ this.tid);
        GlobalVars.fileDeleted[slot] = this.fileDeleted;
    }
}
