package Wiki;

import java.io.File;
import java.util.*;


public class ManageMergers {
    private TreeSet<Integer> emptySlots;
    private int numberOfFiles;
    private boolean writersDone;
    private int numOfThreadCreated;

    private void checkWriter() {
        if (writersDone) return;
        int numofWritersCompleted = 0;
        for (int i = 0; i < GlobalVars.numOfWriterThreads; i++) {
            if (GlobalVars.fileWritten[i] > 0) {
                numofWritersCompleted += 1;
                this.numberOfFiles += GlobalVars.fileWritten[i];
            }
        }
        if (numofWritersCompleted == GlobalVars.numOfWriterThreads) writersDone = true;
    }

    private int getSlot() {
        if (this.emptySlots.isEmpty()) {
            Boolean notGotAnyNewSlots = true;
            while (notGotAnyNewSlots) {
                for (int i = 0; i <= GlobalVars.mergeSlots; i++) {
                    if (GlobalVars.fileDeleted[i] >= 0) {
                        numberOfFiles -= GlobalVars.fileDeleted[i];
                        GlobalVars.fileDeleted[i] = -1;
                        this.emptySlots.add(i);
                        notGotAnyNewSlots = false;
                    }
                }
                try {
                    Thread.sleep(GlobalVars.sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
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
        this.writersDone = false;
        for (int i = 0; i < GlobalVars.mergeSlots; i++) {
            this.emptySlots.add(i);
            GlobalVars.fileDeleted[i] = -1;
        }

    }

    public void start() {
        while (!(this.writersDone && this.numberOfFiles <= 1)) {
            //Task newTask = GlobalVars.taskQueue.poll();
            checkWriter();
            if (this.writersDone)
                System.out.println("Number Of Files in The System " + this.numberOfFiles);
            int slot = getSlot();
            System.out.println("thread " + numOfThreadCreated + "got Slot" + slot);
            Thread mergerThread = new MergerThread(numOfThreadCreated, slot);
            mergerThread.run();
            numOfThreadCreated++;
            try {
                Thread.sleep(GlobalVars.sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
