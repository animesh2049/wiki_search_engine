package Wiki;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

class CustomComparator implements Comparator<Job> {

    public int compare(Job t1, Job t2) {
        if (t1.getFileSize() < t2.getFileSize()) return -1;
        else if (t1.getFileSize() > t2.getFileSize()) return 1;
        else {
            if (t1.getFileName().compareTo(t2.getFileName()) < 0) return -1;
            else if (t1.getFileName().compareTo(t2.getFileName()) > 0) return 1;
            else return 0;
        }
    }
}

public class ManageMergers {
    private int writerDone;
    private PriorityQueue<Job> jobQueue;

    ManageMergers() {
        this.writerDone = 0;
        CustomComparator myComparator = new CustomComparator();
        this.jobQueue = new PriorityQueue<>(10, myComparator);
    }

    public void start() {
        while (true) {
            Task newTask = GlobalVars.taskQueue.poll();
            if (newTask != null) {
                if (newTask.getJobType()) {
                    /*
                        As files become bigger the merge factor should ideally reduce.
                     */
                    Job tempJob;
                    ArrayList<File> filesToMerge;
                    if (this.jobQueue.size() < GlobalVars.mergeFactor) {
//                        newTask
                    }
                    for (int i=0; i < GlobalVars.mergeFactor; i++) {
                        tempJob = this.jobQueue.poll();
                        if (tempJob != null) {

                        }
                    }
                }
                else {
                    if (newTask.getSpecialInfo() != null) this.writerDone += 1;
                    else {
                        this.jobQueue.offer(new Job(newTask.getFileName(), newTask.getFileSize()));
                    }
                }
            }
            else if (this.writerDone == GlobalVars.numOfWriterThreads) {

            }
        }
    }
}
