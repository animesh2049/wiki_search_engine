package Wiki;

import java.io.*;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class MergerThread implements Runnable {
    private Integer tid;
    private String outputFolderPath;
    private PriorityQueue<Entry> pq;
    private final Lock lock;
    private final Condition cond;
    private int localFileCounter;

    MergerThread(Integer tid) {
        this.tid = tid;
        this.outputFolderPath = GlobalVars.tempOutputFolderPath + "/" + this.tid/4 + "/";
        this.pq = new PriorityQueue<>();
        this.lock = new ReentrantLock();
        this.cond = this.lock.newCondition();
        this.localFileCounter = 0;
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

    public void readBlocks(ArrayList<File> filesToMerge) throws IOException {

        PriorityQueue<Entry2> pq2 = new PriorityQueue<>();
        String line;
        ArrayList<BufferedReader> buff = new ArrayList<>(filesToMerge.size());
        int iter=0;
        System.out.println("Now going to merge following files :D");
        for (File temp : filesToMerge) {
            System.out.println(temp.getName());
        }

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
        String newFile = GlobalVars.tempOutputFolderPath + this.tid + "_" + this.localFileCounter;
        try {
            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newFile), "utf-8"));
            this.localFileCounter += 1;
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

        for (File file : filesToMerge) {
            file.delete();
        }
        File newFileObject = new File(newFile);
        Task myJob = new Task(newFile, newFileObject.length(), false);
        GlobalVars.taskQueue.add(myJob);
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
            if (fileList == null) continue;
            try {
                readBlocks(fileList);
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
