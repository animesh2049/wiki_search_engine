package Wiki;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

class WriterThread implements Runnable {
    private Integer tid;
    private Integer docNum;
    private String folderPath;
    HashMap<String, String> tagToShortTag;

    WriterThread(Integer tid, String tempFolderPath) {
        this.tid = tid;
        this.docNum = 0;
        this.folderPath = tempFolderPath;
        this.tagToShortTag = new HashMap<>();
        this.tagToShortTag.put("title", "t");
        this.tagToShortTag.put("infobox", "i");
        this.tagToShortTag.put("text", "d");
        this.tagToShortTag.put("external_links", "e");
        this.tagToShortTag.put("category", "c");
        this.tagToShortTag.put("general_docs", "g");
        this.tagToShortTag.put("comment", "co");
    }

    private void flushToFile(String toWrite) {
        String filePath = this.folderPath + this.tid + "-" + this.docNum;
        try {
            PrintWriter writer = new PrintWriter(filePath, "utf-8");
            writer.print(toWrite);
            writer.close();
        } catch (Exception e) {
            System.err.println("Error while writing file :(");
            e.printStackTrace();
        }
    }

    @Override
    public void run () {
        File myFolder = new File(this.folderPath);
        try {
            myFolder.mkdirs();
        } catch (Exception e) {
            System.err.println("Permission denied :(");
            e.printStackTrace();
            return;
        }
        TreeMap<String, Posting> tempTask;
        while (true) {
            tempTask = GlobalVars.parserWriterBuffer[this.tid/GlobalVars.numOfWriterThreads].poll();
            if (tempTask != null) {
                if (tempTask.get("^end$") != null) {
                    GlobalVars.isParsingDone = true;
                    System.out.println("Got end signal now ending");
                    return;
                }
                StringBuilder toWrite = new StringBuilder("");
                for (Map.Entry<String, Posting> stringPosting : tempTask.entrySet()) {
                    toWrite.append(stringPosting.getKey()).append("=");
                    for (Map.Entry<Integer, TreeMap<String, Integer>> docToListEntry : stringPosting.getValue().docToFreq.entrySet()) {
                        toWrite.append(docToListEntry.getKey()).append(" ");
                        for (Map.Entry<String, Integer> tagToFreqEntry : docToListEntry.getValue().entrySet()) {
                            toWrite.append(this.tagToShortTag.get(tagToFreqEntry.getKey())).append(":").append(tagToFreqEntry.getValue()).append(",");
                        }
                        toWrite.append(";");
                    }
                    toWrite.append("\n");
                }
                flushToFile(toWrite.toString());
                this.docNum += 1;
            }
            else if (GlobalVars.isParsingDone) return;
        }
    }
}

public class MyIndexWriter {
    public void start() {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        for (int i = 0; i < GlobalVars.numOfWriterThreads; i++) {
            WriterThread writerThread = new WriterThread(i, GlobalVars.tempOutputFolderPath);
            executor.execute(writerThread);
        }
        executor.shutdown();
    }

}