package io.github.vipcxj.jasync.ng.runtime.context;

import java.util.*;

public class ContextNMap implements ContextMap {
    private final Map<Object, Object> map;

    public ContextNMap(Map<Object, Object> map) {
        this.map = new HashMap<>(map);
        if (this.map.size() <= 6) {
            throw new IllegalArgumentException("The size of ContextNMap must greater than 6.");
        }
    }

    @Override
    public boolean hasKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public <T> T get(Object key) {
        //noinspection unchecked
        return (T) map.get(key);
    }

    @Override
    public ContextMap put(Object key, Object value) {
        Object o = map.get(key);
        if (Objects.equals(o, value)) {
            return this;
        } else {
            HashMap<Object, Object> newMap = new HashMap<>(map);
            newMap.put(key, value);
            return new ContextNMap(newMap);
        }
    }

    @Override
    public ContextMap remove(Object key) {
        ContextMap.checkKey(key);
        if (map.containsKey(key)) {
            if (size() == 7) {
                Object[] keys = new Object[6];
                Object[] values = new Object[6];
                int i = 0;
                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                    if (!key.equals(entry.getKey())) {
                        keys[i++] = entry.getKey();
                        values[i++] = entry.getValue();
                    }
                }
                return new Context6Map(
                        keys[0], values[0],
                        keys[1], values[1],
                        keys[2], values[2],
                        keys[3], values[3],
                        keys[4], values[4],
                        keys[5], values[5]
                );
            } else if (size() <= 6) {
                throw new IllegalStateException("This is impossible.");
            } else {
                HashMap<Object, Object> newMap = new HashMap<>(map);
                newMap.remove(key);
                return new ContextNMap(newMap);
            }
        } else {
            return this;
        }
    }

    @Override
    public Set<Object> keys() {
        return Collections.unmodifiableSet(map.keySet());
    }
}
