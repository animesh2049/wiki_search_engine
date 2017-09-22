package Wiki;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GlobalVars {
//    public static String xmlFileName = "/home/animesh/Downloads/enwiki-latest-pages-articles-multistream.xml";
    public static String xmlFileName = "/home/animesh/Downloads/wiki-search-small.xml";
    public static String[] mainTags = {"title", "comment", "text", "general_doc"};
    public static String stopWordFile = "/home/animesh/IdeaProjects/wiki_search_engine/src/Wiki/StopWords.txt";
    public static int numOfReaderThreads = 1; // All these numbers should be same so there is a redundancy here.
    public static int numOfWriterThreads = 1; // Remove it later.
    public static int numOfParserThreads = 1;
    public static int numOfMergerThreads = 1;
    public static ConcurrentLinkedQueue< Tuple<Integer, String, String> >[] readerParserBuffer = new ConcurrentLinkedQueue[numOfReaderThreads];
    public static ConcurrentLinkedQueue<TreeMap<String, Posting>>[] parserWriterBuffer = new ConcurrentLinkedQueue[numOfWriterThreads];
   // public static ConcurrentLinkedQueue<Task> taskQueue;
    public static volatile  long [] fileWritten = new long[numOfWriterThreads+1];
    public static ConcurrentLinkedQueue<File> fileMergerBuffer = new ConcurrentLinkedQueue();
    public static volatile boolean isParsingDone;
    public static volatile boolean isMergingDone;
    public static HashMap<String, Boolean> stopWords = new HashMap<>();
    public static int flushFactor = 50;
    public static int mergeFactor = 10;
    public static String tempOutputFolderPath = "/tmp/tempoutput/";
    public static int sizeOfMru = 10000;
    public static Stemmer myStemmer = new Stemmer(sizeOfMru);
//    public static long sleepTime = 200000;
    public static long sleepTime = 20000;
//    public static int estimatedFilesToMerge = 700;
    public static int estimatedFilesToMerge = 100;
//    public static int mergeSlots = 1000;
    public static int mergeSlots = 100;
    public static int Limit = 500;
    public static volatile long [] fileDeleted = new long[mergeSlots];
    public static String secIndexFile = "secIndex.txt";

}
