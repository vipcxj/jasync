package io.github.vipcxj.jasync.ng.runtime.context;

public class Int0Map<T> implements IntMap<T> {

    final static IntMap<Object> EMPTY = new Int0Map<>();

    @Override
    public T get(int key) {
        return null;
    }

    @Override
    public IntMap<T> set(int key, T value) {
        return new Int1Map<>(key, value);
    }

    @Override
    public IntMap<T> remove(int key) {
        return this;
    }
}
