package Wiki;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GlobalVars {
    public static String xmlFileName = "/tmp/test.xml";
    public static String tempFolder = "/tmp/testing";
    public static String[] mainTags = {"title", "comment", "text", "timestamp", "general_doc"};
    public static int numOfReaderThreads = 1; // All these numbers should be same so there is a redundancy here.
    public static int numOfWriterThreads = 1; // Remove it later.
    public static int numOfParserThreads = 1;
    public static ConcurrentLinkedQueue< Tuple<Integer, String, String> >[] readerParserBuffer = new ConcurrentLinkedQueue[numOfReaderThreads];
}
