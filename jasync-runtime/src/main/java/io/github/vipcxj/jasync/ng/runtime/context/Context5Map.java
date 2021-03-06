package io.github.vipcxj.jasync.ng.runtime.context;

import java.util.Objects;
import java.util.Set;

public class Context5Map implements ContextMap {

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
    private Set<Object> keys;

    public Context5Map(Object key1, Object value1, Object key2, Object value2, Object key3, Object value3, Object key4, Object value4, Object key5, Object value5) {
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
    }

    @Override
    public boolean hasKey(Object key) {
        ContextMap.checkKey(key);
        return key.equals(this.key1) || key.equals(this.key2) || key.equals(this.key3) || key.equals(this.key4) || key.equals(this.key5);
    }

    @Override
    public int size() {
        return 5;
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
        } else {
            return null;
        }
    }

    @Override
    public ContextMap put(Object key, Object value) {
        ContextMap.checkKey(key);
        if (key.equals(this.key1)) {
            return Objects.equals(value, value1) ? this : new Context5Map(key, value, this.key2, this.value2, this.key3, this.value3, this.key4, this.value4, this.key5, this.value5);
        } else if (key.equals(this.key2)) {
            return Objects.equals(value, value2) ? this : new Context5Map(this.key1, this.value1, key, value, this.key3, this.value3, this.key4, this.value4, this.key5, this.value5);
        } else if (key.equals(this.key3)) {
            return Objects.equals(value, value3) ? this : new Context5Map(this.key1, this.value1, this.key2, this.value2, key, value, this.key4, this.value4, this.key5, this.value5);
        } else if (key.equals(this.key4)) {
            return Objects.equals(value, value4) ? this : new Context5Map(this.key1, this.value1, this.key2, this.value2, this.key3, this.value3, key, value, this.key5, this.value5);
        } else if (key.equals(this.key5)) {
            return Objects.equals(value, value5) ? this : new Context5Map(this.key1, this.value1, this.key2, this.value2, this.key3, this.value3, this.key4, this.value4, key, value);
        } else {
            return new Context6Map(this.key1, this.value1, this.key2, this.value2, this.key3, this.value3, this.key4, this.value4, this.key5, this.value5, key, value);
        }
    }

    @Override
    public ContextMap remove(Object key) {
        ContextMap.checkKey(key);
        if (key.equals(this.key1)) {
            return new Context4Map(this.key2, this.value2, this.key3, this.value3, this.key4, this.value4, this.key5, this.value5);
        } else if (key.equals(this.key2)) {
            return new Context4Map(this.key1, this.value1, this.key3, this.value3, this.key4, this.value4, this.key5, this.value5);
        } else if (key.equals(this.key3)) {
            return new Context4Map(this.key1, this.value1, this.key2, this.value2, this.key4, this.value4, this.key5, this.value5);
        } else if (key.equals(this.key4)) {
            return new Context4Map(this.key1, this.value1, this.key2, this.value2, this.key3, this.value3, this.key5, this.value5);
        } else if (key.equals(this.key5)) {
            return new Context4Map(this.key1, this.value1, this.key2, this.value2, this.key3, this.value3, this.key4, this.value4);
        } else {
            return this;
        }
    }

    @Override
    public Set<Object> keys() {
        if (keys == null) {
            keys = new ContextMapKeySet(key1, key2, key3, key4, key5);
        }
        return keys;
    }
}
