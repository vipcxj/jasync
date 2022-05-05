package io.github.vipcxj.jasync.ng.runtime.context;

public class IntNMap<T> implements IntMap<T> {

    private final int[] keys;
    private final T[] values;

    public IntNMap(int[] keys, T[] values) {
        this.keys = keys;
        this.values = values;
    }

    @Override
    public T get(int key) {
        for (int i = 0; i < keys.length; ++i) {
            if (key == keys[i]) {
                return values[i];
            }
        }
        return null;
    }

    @Override
    public IntMap<T> set(int key, T value) {
        int pos = -1;
        for (int i = 0; i < keys.length; ++i) {
            if (key == keys[i]) {
                if (value == values[i]) {
                    return this;
                } else {
                    pos = i;
                    break;
                }
            }
        }
        if (pos == -1) {
            int[] newKeys = new int[keys.length + 1];
            System.arraycopy(keys, 0, newKeys, 0, keys.length);
            newKeys[keys.length] = key;
            //noinspection unchecked
            T[] newValues = (T[]) new Object[values.length + 1];
            System.arraycopy(values, 0, newValues, 0, values.length);
            newValues[values.length] = value;
            return new IntNMap<>(newKeys, newValues);
        } else {
            int[] newKeys = new int[keys.length];
            System.arraycopy(keys, 0, newKeys, 0, keys.length);
            newKeys[pos] = key;
            //noinspection unchecked
            T[] newValues = (T[]) new Object[values.length];
            System.arraycopy(values, 0, newValues, 0, values.length);
            newValues[pos] = value;
            return new IntNMap<>(newKeys, newValues);
        }
    }

    @Override
    public IntMap<T> remove(int key) {
        int pos = -1;
        for (int i = 0; i < keys.length; ++i) {
            if (key == keys[i]) {
                pos = i;
                break;
            }
        }
        if (pos == -1) {
            return this;
        } else if (keys.length == 4){
            if (pos == 0) {
                return new Int3Map<>(keys[1], values[1], keys[2], values[2], keys[3], values[3]);
            } else if (pos == 1) {
                return new Int3Map<>(keys[0], values[0], keys[2], values[2], keys[3], values[3]);
            } else if (pos == 2) {
                return new Int3Map<>(keys[0], values[0], keys[1], values[1], keys[3], values[3]);
            } else {
                return new Int3Map<>(keys[0], values[0], keys[1], values[1], keys[2], values[2]);
            }
        } else if (keys.length < 4) {
            throw new IllegalStateException("This is impossible.");
        } else {
            int[] newKeys = new int[keys.length - 1];
            //noinspection unchecked
            T[] newValues = (T[]) new Object[values.length - 1];
            int j = 0;
            for (int i = 0; i < keys.length; ++i) {
                if (i != pos) {
                    newKeys[j] = keys[i];
                    newValues[j++] = values[i];
                }
            }
            return new IntNMap<>(newKeys, newValues);
        }
    }
}
