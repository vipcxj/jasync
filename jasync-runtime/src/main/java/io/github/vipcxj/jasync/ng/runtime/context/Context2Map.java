package io.github.vipcxj.jasync.ng.runtime.context;

import java.util.Objects;
import java.util.Set;

public class Context2Map implements ContextMap {

    private final Object key1;
    private final Object value1;
    private final Object key2;
    private final Object value2;
    private Set<Object> keys;

    public Context2Map(Object key1, Object value1, Object key2, Object value2) {
        this.key1 = key1;
        this.value1 = value1;
        this.key2 = key2;
        this.value2 = value2;
    }

    @Override
    public boolean hasKey(Object key) {
        ContextMap.checkKey(key);
        return key.equals(this.key1) || key.equals(this.key2);
    }

    @Override
    public int size() {
        return 2;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Object key) {
        ContextMap.checkKey(key);
        if (key.equals(this.key1)) {
            return (T) value1;
        } else if (key.equals(this.key2)) {
            return (T) value2;
        } else {
            return null;
        }
    }

    @Override
    public ContextMap put(Object key, Object value) {
        ContextMap.checkKey(key);
        if (key.equals(this.key1)) {
            return Objects.equals(value, value1) ? this : new Context2Map(key, value, this.key2, this.value2);
        } else if (key.equals(this.key2)) {
            return Objects.equals(value, value2) ? this : new Context2Map(this.key1, this.value1, key, value);
        } else {
            return new Context3Map(this.key1, this.value1, this.key2, this.value2, key, value);
        }
    }

    @Override
    public ContextMap remove(Object key) {
        ContextMap.checkKey(key);
        if (key.equals(this.key1)) {
            return new Context1Map(this.key2, this.value2);
        } else if (key.equals(this.key2)) {
            return new Context1Map(this.key1, this.value1);
        } else {
            return this;
        }
    }

    @Override
    public Set<Object> keys() {
        if (keys == null) {
            keys = new ContextMapKeySet(key1, key2);
        }
        return keys;
    }
}
