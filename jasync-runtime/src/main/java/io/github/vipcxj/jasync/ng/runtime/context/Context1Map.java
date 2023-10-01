package io.github.vipcxj.jasync.ng.runtime.context;

import java.util.Set;

public class Context1Map implements ContextMap {

    private final Object key;
    private final Object value;
    private Set<Object> keys;

    public Context1Map(Object key, Object value) {
        ContextMap.checkKey(key);
        this.key = key;
        this.value = value;
    }

    @Override
    public boolean hasKey(Object key) {
        ContextMap.checkKey(key);
        return key.equals(this.key);
    }

    @Override
    public int size() {
        return 1;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Object key) {
        return hasKey(key) ? (T) value : null;
    }

    @Override
    public ContextMap put(Object key, Object value) {
        ContextMap.checkKey(key);
        if (key.equals(this.key)) {
            return new Context1Map(key, value);
        } else {
            return new Context2Map(this.key, this.value, key, value);
        }
    }

    @Override
    public ContextMap remove(Object key) {
        return hasKey(key) ? Context0Map.EMPTY : this;
    }

    @Override
    public Set<Object> keys() {
        if (keys == null) {
            keys = new ContextMapKeySet(key);
        }
        return keys;
    }
}
