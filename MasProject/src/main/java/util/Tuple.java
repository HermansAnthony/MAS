package util;

public class Tuple<K,V> {
    public K first;
    public V second;

    public Tuple(K k, V v) {
        first = k;
        second = v;
    }

    public boolean equals(Tuple<K, V> other) {
        return other.first == this.first && other.second == this.second;
    }

    public String toString() {
        return "<" + first.toString() + "," + second.toString() + ">";
    }
}
