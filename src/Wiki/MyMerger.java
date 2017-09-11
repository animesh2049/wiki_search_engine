package Wiki;

import java.io.*;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

class MergerThread implements Runnable {
    private Integer tid;
    private String outputFolderPath;
    static PriorityQueue<Entry> pq;

    MergerThread(Integer tid) {
        this.tid = tid;
        this.outputFolderPath = GlobalVars.tempOutputFolderPath + "/" + this.tid/4 + "/";
        this.pq = new PriorityQueue<>();
    }

    public void readBlocks(ArrayList<File> filesToMerge) throws IOException {

        PriorityQueue<Entry2> pq2 = new PriorityQueue<Entry2>();
        String fileName = null;
        String line = null;
        ArrayList<BufferedReader> buff = new ArrayList<BufferedReader>(GlobalVars.mergeFactor);
        int iter=0;
        String str2, str1;

        for (File tempFileIter : filesToMerge) {
            try {
                buff.add(new BufferedReader(new FileReader(tempFileIter)));
            }
            catch (FileNotFoundException ex) {
                System.out.println("Unable to open file '" + tempFileIter + "'");
            }
        }
        for (int i=0; i<buff.size(); i++){
            line = buff.get(i).readLine();
            if(line != null) {
                String[] tokens = line.split("=");
                pq.add(new Entry(tokens[0], tokens[1], i));
            }
        }
        try {
            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("z_" + this.tid), "utf-8"));
            Entry first = null, next= null;
            int flag = 0;
            String writeline = "";
            if(pq.size()>0) first = pq.poll();

            line = buff.get(first.value).readLine();
            if(line != null) {
                String[] tokens = line.split("=");
                pq.add(new Entry(tokens[0], tokens[1], first.value));
            }

            while(pq.size()!=0){
                next = pq.poll();
                line = buff.get(next.value).readLine();
                if(line != null) {
                    String[] tokens = line.split("=");
                    pq.add(new Entry(tokens[0], tokens[1], next.value));
                }
                if(next.key.equals(first.key)){
                    pq2.add(new Entry2(first.key,first.posting,first.value));
                    flag = 0;
                    first = next;
                }
                else{
                    pq2.add(new Entry2(first.key,first.posting,first.value));
                    writeline = writeline + first.key+"=";
                    while(pq2.size()!=0){
                        Entry2 temp = pq2.poll();
                        writeline = writeline + temp.posting;
                    }
                    writer.write(writeline+"\n");
                    pq2.clear();
                    writeline = "";
                    first = next;
                    flag = 1;
                }
            }
            if(flag==1){
                writer.write(first.key+"="+first.posting+"\n");
            }
            else{
                pq2.add(new Entry2(first.key,first.posting,first.value));
                writeline = writeline + first.key+"=";
                while(pq2.size()!=0){
                    Entry2 temp = pq2.poll();
                    writeline = writeline + temp.posting;
                }
                writer.write(writeline+"\n");
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        for(int i =0; i<GlobalVars.mergeFactor; i++){
            buff.get(iter).close();
        }

    }

    public void run() {
        File folder = new File(this.outputFolderPath);
        File[] listOfFiles = folder.listFiles();
        ArrayList<File> filesToMerge = new ArrayList<>();
        for (int i=GlobalVars.mergeFactor*this.tid; (i<(GlobalVars.mergeFactor*this.tid)+GlobalVars.mergeFactor) && (i<listOfFiles.length); i++) {
            if (listOfFiles[i].getName() == "z_" + this.tid) continue;
            filesToMerge.add(listOfFiles[i]);
        }
        try {
            readBlocks(filesToMerge);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

public class MyMerger {
    public static void start() {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(GlobalVars.numOfMergerThreads);
        for (int i = 0; i < GlobalVars.numOfMergerThreads; i++) {
            MergerThread merger = new MergerThread(i);
            executor.execute(merger);
        }
        executor.shutdown();
    }
}
