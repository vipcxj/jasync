package io.github.vipcxj.jasync.ng.runtime.context;

public class Int1Map<T> implements IntMap<T> {

    private final int key;
    private final T value;

    public Int1Map(int key, T value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public T get(int key) {
        return key == this.key ? value : null;
    }

    @Override
    public IntMap<T> set(int key, T value) {
        if (key == this.key) {
            return value == this.value ? this : new Int1Map<>(key, value);
        } else {
            return new Int2Map<>(this.key, this.value, key, value);
        }
    }

    @Override
    public IntMap<T> remove(int key) {
        return key == this.key ? IntMap.empty() : this;
    }
}
