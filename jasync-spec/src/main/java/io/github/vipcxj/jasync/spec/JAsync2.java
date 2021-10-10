package io.github.vipcxj.jasync.spec;

import io.github.vipcxj.jasync.spec.spi.JContextProvider;

import java.util.function.Function;

public class JAsync2 {

    static JContextProvider provider = Utils.getProvider(JContextProvider.class);

    public static JPromise2<JContext> context() {
        return provider.current();
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
