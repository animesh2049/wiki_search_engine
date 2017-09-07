package Wiki;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        System.out.println(args.length);
        if (args.length == 0) {
            System.out.println("Usage: main xmlfilename");
            return;
        }
        System.out.println(args[0]);
        GlobalVars.xmlFileName = args[0];
        File xmlFile = new File(GlobalVars.xmlFileName);
        long fileSize = xmlFile.length();
        MyXmlReader myReader = new MyXmlReader(GlobalVars.xmlFileName, fileSize);
        myReader.start();
    }
}
