package io.github.vipcxj.jasync.ng.runtime.context;

public class Int3Map<T> implements IntMap<T> {

    private final int k0;
    private final T v0;
    private final int k1;
    private final T v1;
    private final int k2;
    private final T v2;

    public Int3Map(int k0, T v0, int k1, T v1, int k2, T v2) {
        this.k0 = k0;
        this.v0 = v0;
        this.k1 = k1;
        this.v1 = v1;
        this.k2 = k2;
        this.v2 = v2;
    }

    @Override
    public T get(int key) {
        if (k0 == key) {
            return v0;
        } else if (k1 == key) {
            return v1;
        } else if (k2 == key) {
            return v2;
        } else {
            return null;
        }
    }

    @Override
    public IntMap<T> set(int key, T value) {
        if (k0 == key) {
            return v0 == value ? this : new Int3Map<>(k0, value, k1, v1, k2, v2);
        } else if (k1 == key) {
            return v1 == value ? this : new Int3Map<>(k0, v0, k1, value, k2, v2);
        } else if (k2 == key) {
            return v2 == value ? this : new Int3Map<>(k0, v0, k1, v1, k2, value);
        } else {
            int[] keys = new int[] {k0, k1, k2, key};
            //noinspection unchecked
            T[] values = (T[]) new Object[] {v0, v1, v2, value};
            return new IntNMap<>(keys, values);
        }
    }

    @Override
    public IntMap<T> remove(int key) {
        if (k0 == key) {
            return new Int2Map<>(k1, v1, k2, v2);
        } else if (k1 == key) {
            return new Int2Map<>(k0, v0, k2, v2);
        } else if (k2 == key) {
            return new Int2Map<>(k0, v0, k1, v1);
        } else {
            return this;
        }
    }
}
