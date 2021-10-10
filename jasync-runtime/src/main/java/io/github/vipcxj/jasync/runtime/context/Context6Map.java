package io.github.vipcxj.jasync.runtime.context;

import java.util.Set;

public class Context6Map implements ContextMap {
    private final Object key1;
    private final Object value1;
    private final Object key2;
    private final Object value2;
    private final Object key3;
    private final Object value3;
    private final Object key4;
    private final Object value4;
    private final Object key5;
    private final Object value5;
    private final Object key6;
    private final Object value6;
    private Set<Object> keys;

    public Context6Map(Object key1, Object value1, Object key2, Object value2, Object key3, Object value3, Object key4, Object value4, Object key5, Object value5, Object key6, Object value6) {
        this.key1 = key1;
        this.value1 = value1;
        this.key2 = key2;
        this.value2 = value2;
        this.key3 = key3;
        this.value3 = value3;
        this.key4 = key4;
        this.value4 = value4;
        this.key5 = key5;
        this.value5 = value5;
        this.key6 = key6;
        this.value6 = value6;
    }

    @Override
    public boolean hasKey(Object key) {
        ContextMap.checkKey(key);
        return key.equals(this.key1) || key.equals(this.key2) || key.equals(this.key3) || key.equals(this.key4) || key.equals(this.key5) || key.equals(this.key6);
    }

    @Override
    public int size() {
        return 6;
    }

    @Override
    public <T> T get(Object key) {
        ContextMap.checkKey(key);
        if (key.equals(this.key1)) {
            //noinspection unchecked
            return (T) value1;
        } else if (key.equals(this.key2)) {
            //noinspection unchecked
            return (T) value2;
        } else if (key.equals(this.key3)) {
            //noinspection unchecked
            return (T) value3;
        } else if (key.equals(this.key4)) {
            //noinspection unchecked
            return (T) value4;
        } else if (key.equals(this.key5)) {
            //noinspection unchecked
            return (T) value5;
        } else if (key.equals(this.key6)) {
            //noinspection unchecked
            return (T) value6;
        } else {
            return null;
        }
    }

    @Override
    public ContextMap put(Object key, Object value) {
        return null;
    }

    @Override
    public ContextMap remove(Object key) {
        ContextMap.checkKey(key);
        if (key.equals(this.key1)) {
            return new Context5Map(this.key2, this.value2, this.key3, this.value3, this.key4, this.value4, this.key5, this.value5, this.key6, this.value6);
        } else if (key.equals(this.key2)) {
            return new Context5Map(this.key1, this.value1, this.key3, this.value3, this.key4, this.value4, this.key5, this.value5, this.key6, this.value6);
        } else if (key.equals(this.key3)) {
            return new Context5Map(this.key1, this.value1, this.key2, this.value2, this.key4, this.value4, this.key5, this.value5, this.key6, this.value6);
        } else if (key.equals(this.key4)) {
            return new Context5Map(this.key1, this.value1, this.key2, this.value2, this.key3, this.value3, this.key5, this.value5, this.key6, this.value6);
        } else if (key.equals(this.key5)) {
            return new Context5Map(this.key1, this.value1, this.key2, this.value2, this.key3, this.value3, this.key4, this.value4, this.key6, this.value6);
        } else if (key.equals(this.key6)) {
            return new Context5Map(this.key1, this.value1, this.key2, this.value2, this.key3, this.value3, this.key4, this.value4, this.key5, this.value5);
        } else {
            return this;
        }
    }

    @Override
    public Set<Object> keys() {
        if (keys == null) {
            keys = new ContextMapKeySet(key1, key2, key3, key4, key5, key6);
        }
        return keys;
    }
}
