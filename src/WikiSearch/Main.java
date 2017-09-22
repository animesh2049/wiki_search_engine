package WikiSearch;

import Wiki.GlobalVars;
import Wiki.Pair;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Pattern;

import static java.lang.Math.log;


class CustomComparator implements Comparator<Pair<Double,String>> {

    public int compare(Pair<Double,String> t1, Pair<Double,String > t2) {
        if (t2.getFirst() - t1.getFirst() == 0) return 0;
        return t2.getFirst() > t1.getFirst()?1:-1;
    }
}

public class Main{

    private static Pattern allregex, regexPattern;
    private static ArrayList<String> secIndexFiles;

    public static void main(String[] args) throws Exception {
        regexPattern = Pattern.compile("[^a-zA-Z0-9\\-\']+");
        allregex = Pattern.compile("-{2,}|'{2,}");
        Scanner myScanner = new Scanner(System.in);
        Map<String, String> frequencyMap;
        TreeSet<String> myTree;
        Map<String, Map<String, Map<String, String>>> retrieved = new HashMap<>();
        Map<String, Map<String, Map<String, String>>> docmap = new HashMap<>();
        secIndexFiles = new ArrayList<>();
        initStopWords();
        BufferedReader bufferedReader = new BufferedReader(
                new BufferedReader(new InputStreamReader(new FileInputStream(
                        GlobalVars.tempOutputFolderPath+GlobalVars.secIndexFile)))
        );
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            secIndexFiles.add(line);
        }
        bufferedReader.close();
        int prev;
        int searched;
        ReadFile mapobj = new ReadFile();
        while (true) {
            prev = -1;
            String query = myScanner.nextLine();
            if (query.equals("quit")) return;
            else {
                if (query.contains(":")) {
                    frequencyMap = fieldq2map(query);
                }
                else {
                    frequencyMap = normalq2map(query);
                }
                myTree = new TreeSet<>(frequencyMap.keySet());
                for(String b: myTree){
                    searched = lessequalto(b);
                    if(searched<0) searched = 0;
                    if(prev==searched){
                        if(retrieved.get(b)!=null) {
                            docmap.put(b, retrieved.get(b));
                        }
                    }
                    else{
                        prev = searched;
                        retrieved.clear();
                        retrieved = mapobj.readBlock(String.valueOf(searched)+".txt");
                        if(retrieved.get(b)!=null) {
                            docmap.put(b, retrieved.get(b));
                        }
                    }
                }
                ArrayList<Pair<Double,String>> cosineMap = getCosines(docmap, frequencyMap);
//                int k = 0;
                for (Pair<Double,String> temp: cosineMap ) {
                    System.out.println(temp.getFirst()+" "+temp.getSecond());
//                    if (k > 10) break;
//                    else k += 1;
                }
            }

        }
    }

    private static int lessequalto(String x){
        int index = Collections.binarySearch(secIndexFiles, x);
        if (index < 0)
            return ((-1)*index);
        else
            return index;
    }

    private static String[] processQueryString(String query) {
        String tempQuery = query;
        tempQuery = regexPattern.matcher(tempQuery).replaceAll(" ");
        tempQuery = allregex.matcher(tempQuery).replaceAll("");
        tempQuery = tempQuery.toLowerCase();
        String[] queryTokens = tempQuery.split("\\s");
        ArrayList<String> tokensToProcess = new ArrayList<>();
        for (String temp : queryTokens) {
            if (GlobalVars.stopWords.get(temp) != null) continue; // Stop word removal
            temp = GlobalVars.myStemmer.add(temp);
            tokensToProcess.add(temp);
        }
        return tokensToProcess.toArray(new String[tokensToProcess.size()]);
    }


    private static Map<String, String> fieldq2map (String fq){
        String[] field_tokens = fq.split(" ");
        Map<String, String> fmap = new HashMap<>();
        for(int y=0; y<field_tokens.length; y++) {
            field_tokens[y] = field_tokens[y].toLowerCase();
            if (GlobalVars.stopWords.get(field_tokens[y]) != null) continue; // Stop word removal
            field_tokens[y] = GlobalVars.myStemmer.add(field_tokens[y]);
            String[] fp = field_tokens[y].split(":");
            fp[1]=regexPattern.matcher(fp[1]).replaceAll(" ");
            fp[1]=allregex.matcher(fp[1]).replaceAll(" ");
            fmap.put(fp[1], fp[0]);
        }
        return fmap;
    }

    private static Map<String, String> normalq2map (String nq){
        String[] field_tokens = processQueryString(nq);
        Map<String, String> fmap = new HashMap<>();

        for(String token : field_tokens){
            fmap.put(token, "'f'");
        }
        return fmap;
    }


    private static ArrayList<Pair<Double,String>> getCosines(Map<String, Map<String,
            Map<String, String>>> iim, Map<String, String>  query) {
        int top_k = 10;
        int N = 1000000;
        Map<String, Integer> idf = new HashMap<>();
        ArrayList <Pair<Double,String>> cosine = new ArrayList<>();
        Map<String, ArrayList<TfPair>> doctf = new HashMap<>();
        String keyval;
        String field;
        double score;
        String docids;
        int total;
        int cnt;
        ArrayList<TfPair> list_pair; // size should be query size

        for (Map.Entry<String, String> entry : query.entrySet()) {
            keyval = entry.getKey();
            field = entry.getValue();
            cnt = 0;
            if(iim.get(keyval)==null) {
                continue;
            }
            for (Map.Entry<String, Map<String, String>> wordid : iim.get(keyval).entrySet()){
                docids = wordid.getKey();
                cnt = cnt + 1;
                total = 0;
                for (Map.Entry<String, String> fieldid : iim.get(keyval).get(docids).entrySet()){
                    if(field.equals("'f'")){
                        total = total + Integer.parseInt(fieldid.getValue());
                        continue;
                    }
                    if(fieldid.getKey().equals(field)) {
                        ArrayList<TfPair> value = doctf.get(docids);
                        if (value != null) {
                            value.add(new TfPair(keyval,Integer.parseInt(fieldid.getValue())));
                        } else {
                            doctf.put(docids, new ArrayList<>(20));
                            doctf.get(docids).add(new TfPair(keyval,Integer.parseInt(fieldid.getValue())));
                        }
                        break;
                    }
                }
                if(field.equals("'f'")) {
                    ArrayList<TfPair> value = doctf.get(docids);
                    if (value != null) {
                        value.add(new TfPair(keyval, total));
                    } else {
                        doctf.put(docids, new ArrayList<>(20));
                        doctf.get(docids).add(new TfPair(keyval, total));
                    }
                }
            }
            idf.put(keyval, cnt);
        }
        for (Map.Entry<String, ArrayList<TfPair>> docs : doctf.entrySet()) {
            keyval = docs.getKey();
            list_pair = docs.getValue();
            score = 0.0;
            for(TfPair b : list_pair) {
                if (idf.get(b.word) == 0) score += 0;
                else score  = score + log(1+b.tf)*log(N/idf.get(b.word));
            }
            cosine.add(new Pair(score,keyval));
        }
        Collections.sort(cosine,new CustomComparator());
        return cosine;
    }

    private static void initStopWords() throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(GlobalVars.stopWordFile));
        String line = br.readLine();
        while (line != null) {
            GlobalVars.stopWords.put(line, true);
            line = br.readLine();
        }
    }
}

