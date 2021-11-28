package io.github.vipcxj.jasync.spec;

import io.github.vipcxj.jasync.spec.functional.*;
import io.github.vipcxj.jasync.spec.spi.JPromiseSupport;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("unused")
public interface JPromise2<T> extends JHandle<T> {
    JPromiseSupport provider = Utils.getProvider(JPromiseSupport.class);
    static <T> JPromise2<T> just(T value) {
        return provider.just(value);
    }
    static <T> JPromise2<T> empty() {
        return provider.just(null);
    }
    static <T> JPromise2<T> error(Throwable error) {
        return provider.error(error);
    }
    static JPromise2<Void> sleep(long timeout, TimeUnit unit) {
        return provider.sleep(timeout, unit);
    }
    static <T> JPromise2<T> portal(JAsyncPortalTask1<T> task) {
        return provider.portal(task);
    }
    static <T> JPromise2<T> portal(JAsyncPortalTask0<T> task) {
        return portal((factory, context) -> task.invoke(factory));
    }

    default T await() {
        throw new UnsupportedOperationException("The method \"await\" should be called in an async method.");
    }
    <R> JPromise2<R> thenWithContext(JAsyncPromiseFunction1<T, R> mapper, boolean immediate);
    default <R> JPromise2<R> thenWithContext(JAsyncPromiseFunction1<T, R> mapper) {
        return thenWithContext(mapper, false);
    }
    default <R> JPromise2<R> thenWithContextImmediate(JAsyncPromiseFunction1<T, R> mapper) {
        return thenWithContext(mapper, true);
    }

    default <R> JPromise2<R> then(JAsyncPromiseFunction0<T, R> mapper, boolean immediate) {
        return thenWithContext((v, ctx) -> mapper.apply(v), immediate);
    }
    default <R> JPromise2<R> then(JAsyncPromiseFunction0<T, R> mapper) {
        return then(mapper, false);
    }
    default <R> JPromise2<R> thenImmediate(JAsyncPromiseFunction0<T, R> mapper) {
        return then(mapper, true);
    }

    default <R> JPromise2<R> thenWithContext(JAsyncPromiseSupplier1<R> supplier, boolean immediate) {
        return thenWithContext((ignored, ctx) -> supplier.get(ctx), immediate);
    }
    default <R> JPromise2<R> thenWithContext(JAsyncPromiseSupplier1<R> supplier) {
        return thenWithContext(supplier, false);
    }
    default <R> JPromise2<R> thenWithContextImmediate(JAsyncPromiseSupplier1<R> supplier) {
        return thenWithContext(supplier, true);
    }

    default <R> JPromise2<R> then(JAsyncPromiseSupplier0<R> supplier, boolean immediate) {
        return then((T ignored) -> supplier.get(), immediate);
    }
    default <R> JPromise2<R> then(JAsyncPromiseSupplier0<R> supplier) {
        return then(supplier, false);
    }
    default <R> JPromise2<R> thenImmediate(JAsyncPromiseSupplier0<R> supplier) {
        return then(supplier, true);
    }

    default <R> JPromise2<R> thenMapWithContext(JAsyncFunction1<T, R> function, boolean immediate) {
        return thenWithContext((T v, JContext ctx) -> JPromise2.just(function.apply(v, ctx)), immediate);
    }
    default <R> JPromise2<R> thenMapWithContext(JAsyncFunction1<T, R> function) {
        return thenMapWithContext(function, false);
    }
    default <R> JPromise2<R> thenMapWithContextImmediate(JAsyncFunction1<T, R> function) {
        return thenMapWithContext(function, true);
    }

    default <R> JPromise2<R> thenMap(JAsyncFunction0<T, R> function, boolean immediate) {
        return then((T v) -> JPromise2.just(function.apply(v)), immediate);
    }
    default <R> JPromise2<R> thenMap(JAsyncFunction0<T, R> function) {
        return thenMap(function, false);
    }
    default <R> JPromise2<R> thenMapImmediate(JAsyncFunction0<T, R> function) {
        return thenMap(function, true);
    }

    default <R> JPromise2<R> thenMapWithContext(JAsyncSupplier1<R> function, boolean immediate) {
        return thenWithContext((v, ctx) -> JPromise2.just(function.get(ctx)), immediate);
    }
    default <R> JPromise2<R> thenMapWithContext(JAsyncSupplier1<R> function) {
        return thenMapWithContext(function, false);
    }
    default <R> JPromise2<R> thenMapWithContextImmediate(JAsyncSupplier1<R> function) {
        return thenMapWithContext(function, true);
    }

    default <R> JPromise2<R> thenMap(JAsyncSupplier0<R> function, boolean immediate) {
        return then((T v) -> JPromise2.just(function.get()), immediate);
    }
    default <R> JPromise2<R> thenMap(JAsyncSupplier0<R> function) {
        return thenMap(function, false);
    }
    default <R> JPromise2<R> thenMapImmediate(JAsyncSupplier0<R> function) {
        return thenMap(function, true);
    }

    default <R> JPromise2<T> thenWithWithContext(JAsyncPromiseFunction1<T, R> function, boolean immediate) {
        return thenWithContext((v, ctx) -> function.apply(v, ctx).thenReturn(v), immediate);
    }
    default <R> JPromise2<T> thenWithWithContext(JAsyncPromiseFunction1<T, R> function) {
        return thenWithWithContext(function, false);
    }
    default <R> JPromise2<T> thenWithWithContextImmediate(JAsyncPromiseFunction1<T, R> function) {
        return thenWithWithContext(function, true);
    }

    default <R> JPromise2<T> thenWith(JAsyncPromiseFunction0<T, R> function, boolean immediate) {
        return then(v -> function.apply(v).thenReturn(v), immediate);
    }
    default <R> JPromise2<T> thenWith(JAsyncPromiseFunction0<T, R> function) {
        return thenWith(function, false);
    }
    default <R> JPromise2<T> thenWithImmediate(JAsyncPromiseFunction0<T, R> function) {
        return thenWith(function, true);
    }

    default <R> JPromise2<T> thenWithWithContext(JAsyncPromiseSupplier1<R> supplier, boolean immediate) {
        return thenWithContext((v, ctx) -> supplier.get(ctx).thenReturn(v), immediate);
    }
    default <R> JPromise2<T> thenWithWithContext(JAsyncPromiseSupplier1<R> supplier) {
        return thenWithWithContext(supplier, false);
    }
    default <R> JPromise2<T> thenWithWithContextImmediate(JAsyncPromiseSupplier1<R> supplier) {
        return thenWithWithContext(supplier, true);
    }

    default <R> JPromise2<T> thenWith(JAsyncPromiseSupplier0<R> supplier, boolean immediate) {
        return then(v -> supplier.get().thenReturn(v), immediate);
    }
    default <R> JPromise2<T> thenWith(JAsyncPromiseSupplier0<R> supplier) {
        return thenWith(supplier, false);
    }
    default <R> JPromise2<T> thenWithImmediate(JAsyncPromiseSupplier0<R> supplier) {
        return thenWith(supplier, true);
    }

    default <R> JPromise2<R> thenReturn(R next) {
        return thenMapImmediate(() -> next);
    }

    default JPromise2<T> writeContext(Object key, Object value) {
        return thenWithImmediate(() -> JAsync2.writeContext(key, value));
    }
    default JPromise2<T> pruneContext(Object key) {
        return thenWithImmediate(() -> JAsync2.pruneContext(key));
    }
    default JPromise2<T> updateContext(Object key, T initial, Function<T, T> updater) {
        return thenWithImmediate(() -> JAsync2.updateContext(key, initial, updater));
    }
    default JPromise2<T> updateContextIfExists(Object key, Function<T, T> updater) {
        return thenWithImmediate(() -> JAsync2.updateContextIfExists(key, updater));
    }

    default JPromise2<T> delay(long timeout, TimeUnit unit) {
        return thenWith(() -> sleep(timeout, unit));
    }

    JPromise2<T> doCatchWithContext(JAsyncCatchFunction1<Throwable, T> catcher, boolean immediate);
    default JPromise2<T> doCatchWithContext(JAsyncCatchFunction1<Throwable, T> catcher) {
        return doCatchWithContext(catcher, false);
    }
    default JPromise2<T> doCatchWithContextImmediate(JAsyncCatchFunction1<Throwable, T> catcher) {
        return doCatchWithContext(catcher, true);
    }

    default JPromise2<T> doCatch(JAsyncCatchFunction0<Throwable, T> catcher, boolean immediate) {
        return doCatchWithContext((error, ctx) -> catcher.apply(error), immediate);
    }
    default JPromise2<T> doCatch(JAsyncCatchFunction0<Throwable, T> catcher) {
        return doCatch(catcher, false);
    }
    default JPromise2<T> doCatchImmediate(JAsyncCatchFunction0<Throwable, T> catcher) {
        return doCatch(catcher, true);
    }

    <R> JPromise2<T> doFinallyWithContext(JAsyncPromiseSupplier1<R> supplier, boolean immediate);
    default <R> JPromise2<T> doFinallyWithContext(JAsyncPromiseSupplier1<R> supplier) {
        return doFinallyWithContext(supplier, false);
    }
    default <R> JPromise2<T> doFinallyWithContextImmediate(JAsyncPromiseSupplier1<R> supplier) {
        return doFinallyWithContext(supplier, true);
    }

    default <R> JPromise2<T> doFinally(JAsyncPromiseSupplier0<R> supplier, boolean immediate) {
        return doFinallyWithContext(ctx -> supplier.get(), immediate);
    }
    default <R> JPromise2<T> doFinally(JAsyncPromiseSupplier0<R> supplier) {
        return doFinally(supplier, false);
    }
    default <R> JPromise2<T> doFinallyImmediate(JAsyncPromiseSupplier0<R> supplier) {
        return doFinally(supplier, true);
    }

    @Override
    JPromise2<T> onSuccess(BiConsumer<T, JContext> resolver);
    @Override
    default JPromise2<T> onSuccess(Consumer<T> consumer) {
        return onSuccess((v, ctx) -> consumer.accept(v));
    }
    @Override
    JPromise2<T> onError(BiConsumer<Throwable, JContext> reject);
    @Override
    default JPromise2<T> onError(Consumer<Throwable> consumer) {
        return onError((error, ctx) -> consumer.accept(error));
    }
    @Override
    JPromise2<T> onFinally(Consumer<JContext> consumer);
    @Override
    JPromise2<T> onDispose(Runnable runnable);

    void schedule(JContext context);
    @Override
    void dispose();

    boolean isResolved();
    boolean isRejected();
    default boolean isCompleted() {
        return isResolved() || isRejected();
    }

    void cancel();
    T block(JContext context);
    default T block() {
        return block(JContext.defaultContext());
    }
    JHandle<T> async(JContext context);
    default JHandle<T> async() {
        return async(JContext.defaultContext());
    }
}
