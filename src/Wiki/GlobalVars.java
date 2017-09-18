package Wiki;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GlobalVars {
    public static String xmlFileName = "/home/animesh/Downloads/wiki-search-small.xml";
    public static String[] mainTags = {"title", "comment", "text", "general_doc"};
    public static String stopWordFile = "/home/animesh/IdeaProjects/wiki_search_engine/src/Wiki/StopWords.txt";
    public static int numOfReaderThreads = 1; // All these numbers should be same so there is a redundancy here.
    public static int numOfWriterThreads = 1; // Remove it later.
    public static int numOfParserThreads = 1;
    public static int numOfMergerThreads = 1;
    public static ConcurrentLinkedQueue< Tuple<Integer, String, String> >[] readerParserBuffer = new ConcurrentLinkedQueue[numOfReaderThreads];
    public static ConcurrentLinkedQueue<TreeMap<String, Posting>>[] parserWriterBuffer = new ConcurrentLinkedQueue[numOfWriterThreads];
    public static ConcurrentLinkedQueue<Task> taskQueue;
    public static ConcurrentLinkedQueue<ArrayList<File>>[] fileMergerBuffer = new ConcurrentLinkedQueue[numOfMergerThreads];
    public static boolean isParsingDone;
    public static boolean isMergingDone;
    public static HashMap<String, Boolean> stopWords = new HashMap<>();
    public static int flushFactor = 50;
    public static int mergeFactor = 10;
    public static String tempOutputFolderPath = "/tmp/tempoutput";
    public static int sizeOfMru = 10000;
    public static Stemmer myStemmer = new Stemmer(sizeOfMru);
}
