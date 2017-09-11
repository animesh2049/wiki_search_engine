package Wiki;

public class Entry2 implements Comparable<Entry2> {

    public String key;
    public String posting;
    public int value;

    public Entry2(String key, String posting, int value) {
        this.key = key;
        this.value = value;
        this.posting = posting;
    }

    @Override
    public int compareTo(Entry2 other) {
        return this.value - other.value;
    }
}
