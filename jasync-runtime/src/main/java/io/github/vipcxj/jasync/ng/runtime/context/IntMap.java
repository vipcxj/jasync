package io.github.vipcxj.jasync.ng.runtime.context;

public interface IntMap<T> {

    T get(int key);
    IntMap<T> set(int key, T value);
    IntMap<T> remove(int key);

    static <T> IntMap<T> empty() {
        //noinspection unchecked
        return (IntMap<T>) Int0Map.EMPTY;
    }
}
