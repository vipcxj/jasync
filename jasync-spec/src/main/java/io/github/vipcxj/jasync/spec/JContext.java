package io.github.vipcxj.jasync.spec;

import io.github.vipcxj.jasync.spec.functional.JAsyncPromiseFunction0;
import io.github.vipcxj.jasync.spec.spi.JContextProvider;

import java.util.Optional;
import java.util.Set;

public interface JContext {

    JContextProvider provider = Utils.getProvider(JContextProvider.class);

    static JContext defaultContext() {
        return provider.defaultContext();
    }
    static JPromise2<JContext> current() {
        return provider.current();
    }
    static JPushContext createStackPusher() {
        return provider.createPushContext();
    }
    static <T> JPromise2<T> popStack(JAsyncPromiseFunction0<JStack, T> function) {
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
    JPromise2<JContext> put(Object key, Object value);
    JPromise2<JContext> remove(Object key);
    JScheduler getScheduler();
    JPromise2<JContext> setScheduler(JScheduler scheduler);
    JPromise2<JContext> pushStack(JStack stack);
    JPromise2<JStack> popStack();
}
