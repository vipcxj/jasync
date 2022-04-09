package io.github.vipcxj.jasync.ng.spec;

import io.github.vipcxj.jasync.ng.spec.functional.*;
import io.github.vipcxj.jasync.ng.spec.spi.JPromiseSupport;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("unused")
public interface JPromise<T> extends JHandle<T> {
    JPromiseSupport provider = Utils.getProvider(JPromiseSupport.class);
    static <T> JPromise<T> just(T value) {
        return provider.just(value);
    }
    static <T> JPromise<T> empty() {
        return provider.just(null);
    }
    static <T> JPromise<T> error(Throwable error) {
        return provider.error(error);
    }
    static JPromise<Void> sleep(long timeout, TimeUnit unit) {
        return provider.sleep(timeout, unit);
    }
    static <T> JPromise<T> portal(JAsyncPortalTask1<T> task) {
        return provider.portal(task);
    }
    static <T> JPromise<T> portal(JAsyncPortalTask0<T> task) {
        return portal((factory, context) -> task.invoke(factory));
    }

    /**
     * Create a lazy promise. The handler will be scheduled by the scheduler.
     * @param handler the promise handler.
     * @param <T> the promise type.
     * @return the promise.
     */
    static <T> JPromise<T> create(BiConsumer<JThunk<T>, JContext> handler) {
        return provider.create(handler);
    }
    /**
     * Create a immediate promise. The handler will be invoked in the current thread just after async or block called.
     * @param handler the promise handler.
     * @param <T> the promise type.
     * @return the promise.
     */
    static <T> JPromise<T> generate(BiConsumer<JThunk<T>, JContext> handler) {
        return provider.generate(handler);
    }

    default T await() {
        throw new UnsupportedOperationException("The method \"await\" should be called in an async method.");
    }
    <R> JPromise<R> thenWithContext(JAsyncPromiseFunction1<T, R> mapper, boolean immediate);
    default <R> JPromise<R> thenWithContext(JAsyncPromiseFunction1<T, R> mapper) {
        return thenWithContext(mapper, false);
    }
    default <R> JPromise<R> thenWithContextImmediate(JAsyncPromiseFunction1<T, R> mapper) {
        return thenWithContext(mapper, true);
    }

    default <R> JPromise<R> then(JAsyncPromiseFunction0<T, R> mapper, boolean immediate) {
        return thenWithContext((v, ctx) -> mapper.apply(v), immediate);
    }
    default <R> JPromise<R> then(JAsyncPromiseFunction0<T, R> mapper) {
        return then(mapper, false);
    }
    default <R> JPromise<R> thenImmediate(JAsyncPromiseFunction0<T, R> mapper) {
        return then(mapper, true);
    }

    default <R> JPromise<R> thenWithContext(JAsyncPromiseSupplier1<R> supplier, boolean immediate) {
        return thenWithContext((ignored, ctx) -> supplier.get(ctx), immediate);
    }
    default <R> JPromise<R> thenWithContext(JAsyncPromiseSupplier1<R> supplier) {
        return thenWithContext(supplier, false);
    }
    default <R> JPromise<R> thenWithContextImmediate(JAsyncPromiseSupplier1<R> supplier) {
        return thenWithContext(supplier, true);
    }

    default <R> JPromise<R> then(JAsyncPromiseSupplier0<R> supplier, boolean immediate) {
        return then((T ignored) -> supplier.get(), immediate);
    }
    default <R> JPromise<R> then(JAsyncPromiseSupplier0<R> supplier) {
        return then(supplier, false);
    }
    default <R> JPromise<R> thenImmediate(JAsyncPromiseSupplier0<R> supplier) {
        return then(supplier, true);
    }

    default <R> JPromise<R> thenMapWithContext(JAsyncFunction1<T, R> function, boolean immediate) {
        return thenWithContext((T v, JContext ctx) -> JPromise.just(function.apply(v, ctx)), immediate);
    }
    default <R> JPromise<R> thenMapWithContext(JAsyncFunction1<T, R> function) {
        return thenMapWithContext(function, false);
    }
    default <R> JPromise<R> thenMapWithContextImmediate(JAsyncFunction1<T, R> function) {
        return thenMapWithContext(function, true);
    }

    default <R> JPromise<R> thenMap(JAsyncFunction0<T, R> function, boolean immediate) {
        return then((T v) -> JPromise.just(function.apply(v)), immediate);
    }
    default <R> JPromise<R> thenMap(JAsyncFunction0<T, R> function) {
        return thenMap(function, false);
    }
    default <R> JPromise<R> thenMapImmediate(JAsyncFunction0<T, R> function) {
        return thenMap(function, true);
    }

    default <R> JPromise<R> thenMapWithContext(JAsyncSupplier1<R> function, boolean immediate) {
        return thenWithContext((v, ctx) -> JPromise.just(function.get(ctx)), immediate);
    }
    default <R> JPromise<R> thenMapWithContext(JAsyncSupplier1<R> function) {
        return thenMapWithContext(function, false);
    }
    default <R> JPromise<R> thenMapWithContextImmediate(JAsyncSupplier1<R> function) {
        return thenMapWithContext(function, true);
    }

    default <R> JPromise<R> thenMap(JAsyncSupplier0<R> function, boolean immediate) {
        return then((T v) -> JPromise.just(function.get()), immediate);
    }
    default <R> JPromise<R> thenMap(JAsyncSupplier0<R> function) {
        return thenMap(function, false);
    }
    default <R> JPromise<R> thenMapImmediate(JAsyncSupplier0<R> function) {
        return thenMap(function, true);
    }

    default <R> JPromise<T> thenWithWithContext(JAsyncPromiseFunction1<T, R> function, boolean immediate) {
        return thenWithContext((v, ctx) -> function.apply(v, ctx).thenReturn(v), immediate);
    }
    default <R> JPromise<T> thenWithWithContext(JAsyncPromiseFunction1<T, R> function) {
        return thenWithWithContext(function, false);
    }
    default <R> JPromise<T> thenWithWithContextImmediate(JAsyncPromiseFunction1<T, R> function) {
        return thenWithWithContext(function, true);
    }

    default <R> JPromise<T> thenWith(JAsyncPromiseFunction0<T, R> function, boolean immediate) {
        return then(v -> function.apply(v).thenReturn(v), immediate);
    }
    default <R> JPromise<T> thenWith(JAsyncPromiseFunction0<T, R> function) {
        return thenWith(function, false);
    }
    default <R> JPromise<T> thenWithImmediate(JAsyncPromiseFunction0<T, R> function) {
        return thenWith(function, true);
    }

    default <R> JPromise<T> thenWithWithContext(JAsyncPromiseSupplier1<R> supplier, boolean immediate) {
        return thenWithContext((v, ctx) -> supplier.get(ctx).thenReturn(v), immediate);
    }
    default <R> JPromise<T> thenWithWithContext(JAsyncPromiseSupplier1<R> supplier) {
        return thenWithWithContext(supplier, false);
    }
    default <R> JPromise<T> thenWithWithContextImmediate(JAsyncPromiseSupplier1<R> supplier) {
        return thenWithWithContext(supplier, true);
    }

    default <R> JPromise<T> thenWith(JAsyncPromiseSupplier0<R> supplier, boolean immediate) {
        return then(v -> supplier.get().thenReturn(v), immediate);
    }
    default <R> JPromise<T> thenWith(JAsyncPromiseSupplier0<R> supplier) {
        return thenWith(supplier, false);
    }
    default <R> JPromise<T> thenWithImmediate(JAsyncPromiseSupplier0<R> supplier) {
        return thenWith(supplier, true);
    }

    default <R> JPromise<R> thenReturn(R next) {
        return thenMapImmediate(() -> next);
    }

    default JPromise<T> writeContext(Object key, Object value) {
        return thenWithImmediate(() -> JAsync.writeContext(key, value));
    }
    default JPromise<T> pruneContext(Object key) {
        return thenWithImmediate(() -> JAsync.pruneContext(key));
    }
    default JPromise<T> updateContext(Object key, T initial, Function<T, T> updater) {
        return thenWithImmediate(() -> JAsync.updateContext(key, initial, updater));
    }
    default JPromise<T> updateContextIfExists(Object key, Function<T, T> updater) {
        return thenWithImmediate(() -> JAsync.updateContextIfExists(key, updater));
    }

    default JPromise<T> delay(long timeout, TimeUnit unit) {
        return thenWith(() -> sleep(timeout, unit));
    }

    JPromise<T> doCatchWithContext(JAsyncCatchFunction1<Throwable, T> catcher, boolean immediate);
    default JPromise<T> doCatchWithContext(JAsyncCatchFunction1<Throwable, T> catcher) {
        return doCatchWithContext(catcher, false);
    }
    default JPromise<T> doCatchWithContextImmediate(JAsyncCatchFunction1<Throwable, T> catcher) {
        return doCatchWithContext(catcher, true);
    }

    default JPromise<T> doCatch(JAsyncCatchFunction0<Throwable, T> catcher, boolean immediate) {
        return doCatchWithContext((error, ctx) -> catcher.apply(error), immediate);
    }
    default JPromise<T> doCatch(JAsyncCatchFunction0<Throwable, T> catcher) {
        return doCatch(catcher, false);
    }
    default JPromise<T> doCatchImmediate(JAsyncCatchFunction0<Throwable, T> catcher) {
        return doCatch(catcher, true);
    }

    <R> JPromise<T> doFinallyWithContext(JAsyncPromiseSupplier1<R> supplier, boolean immediate);
    default <R> JPromise<T> doFinallyWithContext(JAsyncPromiseSupplier1<R> supplier) {
        return doFinallyWithContext(supplier, false);
    }
    default <R> JPromise<T> doFinallyWithContextImmediate(JAsyncPromiseSupplier1<R> supplier) {
        return doFinallyWithContext(supplier, true);
    }

    default <R> JPromise<T> doFinally(JAsyncPromiseSupplier0<R> supplier, boolean immediate) {
        return doFinallyWithContext(ctx -> supplier.get(), immediate);
    }
    default <R> JPromise<T> doFinally(JAsyncPromiseSupplier0<R> supplier) {
        return doFinally(supplier, false);
    }
    default <R> JPromise<T> doFinallyImmediate(JAsyncPromiseSupplier0<R> supplier) {
        return doFinally(supplier, true);
    }

    @Override
    JPromise<T> onSuccess(BiConsumer<T, JContext> resolver);
    @Override
    default JPromise<T> onSuccess(Consumer<T> consumer) {
        return onSuccess((v, ctx) -> consumer.accept(v));
    }
    @Override
    JPromise<T> onError(BiConsumer<Throwable, JContext> reject);
    @Override
    default JPromise<T> onError(Consumer<Throwable> consumer) {
        return onError((error, ctx) -> consumer.accept(error));
    }
    @Override
    JPromise<T> onFinally(Consumer<JContext> consumer);
    @Override
    JPromise<T> onDispose(Runnable runnable);

    void schedule(JContext context);
    @Override
    void dispose();

    boolean isResolved();
    boolean isRejected();
    default boolean isCompleted() {
        return isResolved() || isRejected();
    }

    void cancel();
    T block(JContext context) throws InterruptedException;
    default T block() throws InterruptedException {
        return block(JContext.defaultContext());
    }
    JHandle<T> async(JContext context);
    default JHandle<T> async() {
        return async(JContext.defaultContext());
    }
}
