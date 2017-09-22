package WikiSearch;

import Wiki.GlobalVars;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class ReadFile {

    public Map<String, Map<String, Map<String, String>>> readBlock(String file) {
        String baseDir = GlobalVars.tempOutputFolderPath;
        //String fileName = baseDir+"inverted_index_small.txt";
        String fileName = baseDir + file;

        Map<String, Map<String, Map<String, String>>> perMap = new HashMap<>();
        String line;
        try {
            FileReader fileReader =
                    new FileReader(fileName);

            BufferedReader bufferedReader =
                    new BufferedReader(fileReader);

            while ((line = bufferedReader.readLine()) != null) {
                String[] tokens = line.split("=");
                perMap.put(tokens[0], str2Map(tokens[1]));
            }
            bufferedReader.close();
        }
        catch (FileNotFoundException ex) {
            System.out.println(
                    "Unable to open file '" +
                            fileName + "'");
        }
        catch (IOException ex) {
            System.out.println(
                    "Error reading file '"
                            + fileName + "'");
            // ex.printStackTrace();
        }

        return perMap;
    }

    private static Map<String, Map<String, String>> str2Map(String str) {
        String[] tokens = str.split(" |;");
        String[] subtokens;
        Map<String, Map<String, String>> map = new HashMap<>();
        Map<String, String> submap = new HashMap<>();
        for (int i = 0; i < tokens.length - 1; i += 2) {
            subtokens = tokens[i + 1].split(",|:");
            for (int j = 0; j < subtokens.length - 1; ) {
                submap.put(subtokens[j++], subtokens[j++]);
            }
            map.put(tokens[i], submap);
            submap = new HashMap<>();
        }
        return map;
    }

}
