package io.github.vipcxj.jasync.ng.spec;

import io.github.vipcxj.jasync.ng.spec.annotations.Internal;
import io.github.vipcxj.jasync.ng.spec.functional.JAsyncPromiseFunction0;
import io.github.vipcxj.jasync.ng.spec.spi.JContextProvider;

import java.util.Optional;
import java.util.Set;

public interface JContext {

    JContextProvider provider = Utils.getProvider(JContextProvider.class);

    static JContext defaultContext() {
        return provider.defaultContext();
    }
    static JContext create(JScheduler scheduler) {
        return provider.create(scheduler);
    }
    static JPushContext createStackPusher() {
        return provider.createPushContext();
    }
    static <T> JPromise<T> popStack(JAsyncPromiseFunction0<JStack, T> function) {
        return JPromise.context().thenImmediate(JContext::popStack).thenImmediate(function);
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
    JScheduler getScheduler();
    JContext setScheduler(JScheduler scheduler);
    @Internal
    JContext pushStack(JStack stack);
    @Internal
    JPromise<JStack> popStack();
}
