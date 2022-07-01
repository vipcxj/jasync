package io.github.vipcxj.jasync.ng.spec;

import io.github.vipcxj.jasync.ng.spec.annotations.Internal;
import io.github.vipcxj.jasync.ng.spec.spi.JContextProvider;

import java.util.Optional;
import java.util.Set;

public interface JContext {

    JContextProvider provider = Utils.getProvider(JContextProvider.class);

    static JContext defaultContext() {
        return provider.defaultContext();
    }
    static JContext create(JScheduler scheduler, boolean supportStackTrace) {
        return provider.create(scheduler, supportStackTrace);
    }
    default JContext create(JScheduler scheduler) {
        return create(scheduler, true);
    }
    default JContext create(boolean supportStackTrace) {
        return create(JScheduler.defaultScheduler(), supportStackTrace);
    }
    <T> T get(Object key);
    JContext set(Object key, Object value);
    JContext remove(Object key);
    boolean hasKey(Object key);
    Set<Object> keys();
    default <T> T getOrDefault(Object key, T defaultValue) {
        if (hasKey(key)) {
            return get(key);
        } else {
            return defaultValue;
        }
    }
    default <T> Optional<T> getOrEmpty(Object key) {
        if (hasKey(key)) {
            return Optional.of(get(key));
        } else {
            return Optional.empty();
        }
    }
    int size();
    default boolean isEmpty() {
        return size() == 0;
    }

    @Internal
    Object[] getLocals();
    @Internal
    JContext pushLocals(Object... args);
    @Internal
    JContext popLocals();
    @Internal
    JContext setPortal(int jumpIndex, JPortal<?> portal);
    @Internal
    <T> JPromise<T> jump(int jumpIndex);
    @Internal
    JContext removePortal(int jumpIndex);
    JScheduler getScheduler();
    JContext setScheduler(JScheduler scheduler);
    @Internal
    JContext pushStackFrame(String declaringClass, String method, String fileName);
    @Internal
    JContext setLineNumber(int lineNumber);
    @Internal
    JContext popStackFrame();
    @Internal
    JContext cloneMutable();
    void fixException(Throwable throwable);
}
