package Wiki;

import java.util.PriorityQueue;

public class Entry implements Comparable<Entry> {
    public static void main(String[] args){
        PriorityQueue<Entry> q = new PriorityQueue<>();
        PriorityQueue<Entry> q2 = new PriorityQueue<>();
        q.add(new Entry("google","hdahf", 3));
        q.add(new Entry("hike", "adfa",2));
        q.add(new Entry("apple", "dshaf", 3));
        q.poll();
        q.add(new Entry("google","hdahf", 1));
        int l = q.size();
        for(int i=0 ; i<l; i++){
            Entry temp = q.poll();
            System.out.print(temp.key+" : ");
            System.out.println(temp.value);
        }
        q.add(new Entry("az","dad", 3));

    }
    public String key;
    public String posting;
    public int value;

    public Entry(String key, String posting, int value) {
        this.key = key;
        this.value = value;
        this.posting = posting;
    }

    @Override
    public int compareTo(Entry other) {
        return this.key.compareTo(other.key);
    }
}
