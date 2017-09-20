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


    public void start() {
        while (true) {
            //Task newTask = GlobalVars.taskQueue.poll();

            Thread.sleep(GlobalVars.sleepTime);
            if (newTask != null) {
                if (newTask.getJobType()) {
                    /*
                     As files become bigger the merge factor should ideally reduce.
                     */
                    Job tempJob;
                    ArrayList<File> filesToMerge = new ArrayList<>();
                    for (int i=0; i < GlobalVars.mergeFactor; i++) {
                        tempJob = this.jobQueue.poll();
                        if (tempJob != null) {
                            System.out.println(tempJob.getFileName() + " " + tempJob.getFileSize());
                            filesToMerge.add(new File(tempJob.getFileName()));
                        }
                        else break;
                    }
                    System.out.println("Done");
                    if (filesToMerge.size() > 1) {
                        GlobalVars.fileMergerBuffer[newTask.getTid()].add(filesToMerge);
                    }
                    filesToMerge.clear();
                    newTask.getWhoSlept().getLock().lock();
                    newTask.getWhoSlept().getCond().signal();
                    newTask.getWhoSlept().getLock().unlock();
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
