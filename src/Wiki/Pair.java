package Wiki;

public class Pair<L, R> {
    private final L first;
    private final R second;

    public Pair(L first, R second) {
        this.first = first;
        this.second = second;
    }

    public L getFirst() {return this.first;}
    public R getSecond() {return this.second;}

    @Override
    public int hashCode() {return first.hashCode()^second.hashCode();}

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Pair)) return false;
        Pair temp = (Pair) o;
        return ((this.first==((Pair) o).getFirst()) && (this.second==((Pair) o).getSecond()));
    }

}
