package io.github.vipcxj.jasync.ng.spec;

import io.github.vipcxj.jasync.ng.spec.functional.JAsyncPromiseFunction0;
import io.github.vipcxj.jasync.ng.spec.spi.JContextProvider;

import java.util.Optional;
import java.util.Set;

public interface JContext {

    JContextProvider provider = Utils.getProvider(JContextProvider.class);

    static JContext defaultContext() {
        return provider.defaultContext();
    }
    static JPromise<JContext> current() {
        return provider.current();
    }
    static JPushContext createStackPusher() {
        return provider.createPushContext();
    }
    static <T> JPromise<T> popStack(JAsyncPromiseFunction0<JStack, T> function) {
        return current().thenImmediate(context -> context.popStack()).thenImmediate(function);
    }

    <T> T get(Object key);
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
    JPromise<JContext> put(Object key, Object value);
    JPromise<JContext> remove(Object key);
    JScheduler getScheduler();
    JPromise<JContext> setScheduler(JScheduler scheduler);
    JPromise<JContext> pushStack(JStack stack);
    JPromise<JStack> popStack();
}
