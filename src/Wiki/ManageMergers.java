package Wiki;

import java.util.*;


public class ManageMergers {
    private TreeSet<Integer> emptySlots;
    private int numberOfFiles;
    private boolean writersDone;
    private int numOfThreadCreated;
    private int startIter;

    private void checkWriter() {
        if (this.writersDone) return;
        int numofWritersCompleted = 0;
        for (int i = 0; i < GlobalVars.numOfWriterThreads; i++) {
            if (GlobalVars.fileWritten[i] > 0) {
                numofWritersCompleted += 1;
                this.numberOfFiles += GlobalVars.fileWritten[i];
            }
        }
        if (numofWritersCompleted == GlobalVars.numOfWriterThreads) this.writersDone = true;
        System.out.println("writers done is " + this.writersDone + " numOfFiles " + this.numberOfFiles);
    }
    private void getReport() {
        for (int i = 0; i < GlobalVars.Limit; i++) {
            this.startIter = (this.startIter + 1) % GlobalVars.mergeSlots;
            if (GlobalVars.fileDeleted[this.startIter] >= 0) {
                this.numberOfFiles -= GlobalVars.fileDeleted[this.startIter];
                GlobalVars.fileDeleted[this.startIter] = -1;
                this.emptySlots.add(this.startIter);
            }
        }
    }
    private int getSlot() {
        getReport();
        if (this.emptySlots.isEmpty()) {
            try {
                Thread.sleep(GlobalVars.sleepTime);
                return getSlot();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (this.emptySlots.isEmpty()) {
            System.out.println("Ye jaane kab ye hua kaise Hua");
            return 0;
        }
        return this.emptySlots.pollFirst();
    }

    ManageMergers() {
        this.emptySlots = new TreeSet<>();
        this.numberOfFiles = 0;
        this.startIter = 0;
        this.writersDone = false;
        for (int i = 0; i < GlobalVars.mergeSlots; i++) {
            this.emptySlots.add(i);
            GlobalVars.fileDeleted[i] = -1;
        }

    }

    public void start() {
        while (!(this.writersDone && this.numberOfFiles <= 1)) {
            System.out.println("Try next thread");
            //Task newTask = GlobalVars.taskQueue.poll();
            checkWriter();
            if (this.writersDone)
                System.out.println("Number Of Files in The System " + this.numberOfFiles);
            int slot = getSlot();
            System.out.println("thread " + numOfThreadCreated + " got Slot" + slot);
            Thread mergerThread = new MergerThread(numOfThreadCreated, slot);
            mergerThread.start();
            numOfThreadCreated++;
            try {
                Thread.sleep(GlobalVars.sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Done");
    }
}
