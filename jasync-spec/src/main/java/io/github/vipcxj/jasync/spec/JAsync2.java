package io.github.vipcxj.jasync.spec;

import io.github.vipcxj.jasync.spec.spi.JContextProvider;
import io.github.vipcxj.jasync.spec.spi.JPromiseSupport;

import java.util.List;
import java.util.function.Function;

public class JAsync2 {

    static JPromiseSupport promiseProvider = Utils.getProvider(JPromiseSupport.class);
    static JContextProvider contextProvider = Utils.getProvider(JContextProvider.class);

    static  <T> JPromise2<T> any(JPromise2<? extends T>... promises) {
        return promiseProvider.any(promises);
    }
    static  <T> JPromise2<T> any(List<JPromise2<? extends T>> promises) {
        return promiseProvider.any(promises);
    }
    static <T> JPromise2<List<T>> all(List<JPromise2<? extends T>> promises) {
        return promiseProvider.all(promises);
    }
    static  <T> JPromise2<List<T>> all(JPromise2<? extends T>... promises) {
        return promiseProvider.all(promises);
    }

    public static JPromise2<JContext> context() {
        return contextProvider.current();
    }

    public static <T> JPromise2<T> getContextValue(Object key) {
        return context().thenMap(ctx -> ctx.get(key));
    }

    public static <T> JPromise2<T> getContextValue(Object key, T defaultValue) {
        return context().thenMap(ctx -> ctx.getOrDefault(key, defaultValue));
    }

    public static JPromise2<JContext> writeContext(Object key, Object value) {
        return context().then((JContext ctx) -> ctx.put(key, value), true);
    }

    public static JPromise2<JContext> pruneContext(Object key) {
        return context().then((JContext ctx) -> ctx.remove(key), true);
    }

    public static <T> JPromise2<JContext> updateContext(Object key, T initial, Function<T, T> updater) {
        return context().then((JContext ctx) -> {
            if (ctx.hasKey(key)) {
                return ctx.put(key, updater.apply(ctx.get(key)));
            } else {
                return ctx.put(key, initial);
            }
        }, true);
    }

    public static <T> JPromise2<JContext> updateContextIfExists(Object key, Function<T, T> updater) {
        return context().then((JContext ctx) -> {
            if (ctx.hasKey(key)) {
                return ctx.put(key, updater.apply(ctx.get(key)));
            } else {
                return JPromise2.just(ctx);
            }
        }, true);
    }

    public static JPromise2<JContext> setScheduler(JScheduler scheduler) {
        return context().then((JContext ctx) -> ctx.setScheduler(scheduler), true);
    }
}
