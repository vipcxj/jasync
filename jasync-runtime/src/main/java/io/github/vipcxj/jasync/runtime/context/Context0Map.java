package io.github.vipcxj.jasync.runtime.context;

import java.util.Set;

public class Context0Map implements ContextMap {

    public static final ContextMap EMPTY = new Context0Map();

    private Set<Object> keys = new ContextMapKeySet();

    private Context0Map() {}

    @Override
    public <T> T get(Object key) {
        ContextMap.checkKey(key);
        return null;
    }

    @Override
    public boolean hasKey(Object key) {
        ContextMap.checkKey(key);
        return false;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public ContextMap put(Object key, Object value) {
        ContextMap.checkKey(key);
        return new Context1Map(key, value);
    }

    @Override
    public ContextMap remove(Object key) {
        ContextMap.checkKey(key);
        return this;
    }

    @Override
    public Set<Object> keys() {
        return keys;
    }
}
