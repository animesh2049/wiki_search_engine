package Wiki;

public class Tuple<L, R, T> {
    private final L first;
    private final R second;
    private final T third;

    public Tuple(L first, R second, T third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public L getFirst() {return this.first;}
    public R getSecond() {return this.second;}
    public T getThird() {return this.third;}

    @Override
    public int hashCode() {return first.hashCode()^second.hashCode();}

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Tuple)) return false;
        Tuple temp = (Tuple) o;
        return ((this.first==((Tuple) o).getFirst()) && (this.second==((Tuple) o).getSecond()) && (this.third==((Tuple) o).getThird()));
    }
}
