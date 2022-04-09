package io.github.vipcxj.jasync.ng.runtime.context;

import java.util.Objects;
import java.util.Set;

public interface ContextMap {

    boolean hasKey(Object key);
    int size();
    <T> T get(Object key);
    ContextMap put(Object key, Object value);
    ContextMap remove(Object key);
    Set<Object> keys();
    static void checkKey(Object key) {
        Objects.requireNonNull(key, "The key of the context must be non null.");
    }
}
