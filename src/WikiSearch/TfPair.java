package WikiSearch;

public class TfPair {
    public String word;
    public int tf;

    public TfPair(String key,int value) {
        this.word = key;
        this.tf = value;
    }
}
