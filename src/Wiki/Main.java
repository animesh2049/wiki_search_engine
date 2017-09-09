package Wiki;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Main {
    public static void main(String[] args) {
        System.out.println(args.length);
        if (args.length == 0) {
            System.out.println("Usage: main xmlfilename");
            return;
        }
        System.out.println(args[0]);
        GlobalVars.xmlFileName = args[0];
        for (int i = 0; i < GlobalVars.numOfReaderThreads; i++) {
            GlobalVars.readerParserBuffer[i] = new ConcurrentLinkedQueue<>();
        }
        File xmlFile = new File(GlobalVars.xmlFileName);
        long fileSize = xmlFile.length();
        MyXmlReader myReader = new MyXmlReader(GlobalVars.xmlFileName, fileSize);
        myReader.start();
    }
}
