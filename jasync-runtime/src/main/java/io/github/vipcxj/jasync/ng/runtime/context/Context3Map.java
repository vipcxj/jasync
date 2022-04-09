package io.github.vipcxj.jasync.ng.runtime.context;

import java.util.Set;

public class Context3Map implements ContextMap {

    private final Object key1;
    private final Object value1;
    private final Object key2;
    private final Object value2;
    private final Object key3;
    private final Object value3;
    private Set<Object> keys;

    public Context3Map(Object key1, Object value1, Object key2, Object value2, Object key3, Object value3) {
        this.key1 = key1;
        this.value1 = value1;
        this.key2 = key2;
        this.value2 = value2;
        this.key3 = key3;
        this.value3 = value3;
    }

    @Override
    public boolean hasKey(Object key) {
        ContextMap.checkKey(key);
        return key.equals(this.key1) || key.equals(this.key2) || key.equals(this.key3);
    }

    @Override
    public int size() {
        return 3;
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
        } else {
            return null;
        }
    }

    @Override
    public ContextMap put(Object key, Object value) {
        ContextMap.checkKey(key);
        if (key.equals(this.key1)) {
            return new Context3Map(key, value, this.key2, this.value2, this.key3, this.value3);
        } else if (key.equals(this.key2)) {
            return new Context3Map(this.key1, this.value1, key, value, this.key3, this.value3);
        } else if (key.equals(this.key3)) {
            return new Context3Map(this.key1, this.value1, this.key2, this.value2, key, value);
        } else {
            return new Context4Map(this.key1, this.value1, this.key2, this.value2, this.key3, this.value3, key, value);
        }
    }

    @Override
    public ContextMap remove(Object key) {
        ContextMap.checkKey(key);
        if (key.equals(this.key1)) {
            return new Context2Map(this.key2, this.value2, this.key3, this.value3);
        } else if (key.equals(this.key2)) {
            return new Context2Map(this.key1, this.value1, this.key3, this.value3);
        } else if (key.equals(this.key3)) {
            return new Context2Map(this.key1, this.value1, this.key2, this.value2);
        } else {
            return this;
        }
    }

    @Override
    public Set<Object> keys() {
        if (keys == null) {
            keys = new ContextMapKeySet(key1, key2, key3);
        }
        return keys;
    }
}
