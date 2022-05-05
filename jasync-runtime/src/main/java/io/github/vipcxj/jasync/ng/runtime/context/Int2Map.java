package io.github.vipcxj.jasync.ng.runtime.context;

public class Int2Map<T> implements IntMap<T> {

    private final int k0;
    private final T v0;
    private final int k1;
    private final T v1;

    public Int2Map(int k0, T v0, int k1, T v1) {
        this.k0 = k0;
        this.v0 = v0;
        this.k1 = k1;
        this.v1 = v1;
    }

    @Override
    public T get(int key) {
        if (k0 == key) {
            return v0;
        } else if (k1 == key) {
            return v1;
        } else {
            return null;
        }
    }

    @Override
    public IntMap<T> set(int key, T value) {
        if (k0 == key) {
            return v0 == value ? this : new Int2Map<>(k0, value, k1, v1);
        } else if (k1 == key) {
            return v1 == value ? this : new Int2Map<>(k0, v0, k1, value);
        } else {
            return new Int3Map<>(k0, v0, k1, v1, key, value);
        }
    }

    @Override
    public IntMap<T> remove(int key) {
        if (k0 == key) {
            return new Int1Map<>(k1, v1);
        } else if (k1 == key) {
            return new Int1Map<>(k0, v0);
        } else {
            return this;
        }
    }
}
