package io.github.vipcxj.jasync.ng.spec;

import io.github.vipcxj.jasync.ng.spec.functional.*;
import io.github.vipcxj.jasync.ng.spec.spi.JPromiseSupport;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("unused")
public interface JPromise<T> extends JHandle<T> {
    JPromiseSupport provider = Utils.getProvider(JPromiseSupport.class);
    static int genId() {
        return provider.generateId();
    }
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
    static <T> JPromise<T> portal(JAsyncPromiseFunction0<Object[], T> task, int jumpIndex, Object... locals) {
        JPromise<T> promise = updateContext(ctx -> ctx.pushLocals(locals))
                .thenImmediate(() ->
                        portal((factory, context) -> {
                                    Object[] theLocals = context.getLocals();
                                    return updateContext(ctx -> ctx.popLocals().setPortal(jumpIndex, factory))
                                            .thenImmediate(() -> task.apply(theLocals));
                                }
                        ));
        return promise.withUpdateContext(ctx -> ctx.removePortal(jumpIndex));
    }
    static <T> JPromise<T> portal(JAsyncPromiseFunction1<Object[], T> task, int jumpIndex, Object... locals) {
        JPromise<T> promise = updateContext(ctx -> ctx.pushLocals(locals))
                .thenImmediate(() ->
                        portal((factory, context) -> {
                                    Object[] theLocals = context.getLocals();
                                    return updateContext(ctx -> ctx.popLocals().setPortal(jumpIndex, factory))
                                            .thenWithContextImmediate(ctx -> task.apply(theLocals, ctx));
                                }
                        ));
        return promise.withUpdateContext(ctx -> ctx.removePortal(jumpIndex));
    }
    static <T> JPromise<T> jump(int jumpIndex, Object... localVars) {
        return updateContext(ctx -> ctx.pushLocals(localVars)).thenImmediate(ctx -> ctx.jump(jumpIndex));
    }
    static JAsyncReadWriteLock readWriteLock() {
        return provider.readWriteLock();
    }
    static <T> JPromise<T> methodDebugInfo(JAsyncPromiseSupplier1<T> supplier, String declaringClassQualifiedName, String method, String fileName) {
        return updateContext(ctx -> ctx.pushStackFrame(declaringClassQualifiedName, method, fileName)).thenWithContextImmediate(supplier).withUpdateContext(JContext::popStackFrame);
    }
    static <T> JPromise<T> wrap(JAsyncPromiseSupplier0<T> supplier) {
        return JPromise.empty().thenImmediate(supplier);
    }
    static <T> JPromise<T> wrap(JAsyncPromiseSupplier1<T> supplier) {
        return JPromise.empty().thenWithContextImmediate(supplier);
    }
    @SafeVarargs
    static  <T> JPromise<T> any(JPromise<? extends T>... promises) {
        return provider.any(promises);
    }
    static  <T> JPromise<T> any(List<JPromise<? extends T>> promises) {
        return provider.any(promises);
    }
    @SafeVarargs
    static  <T> JPromise<T> race(JPromise<? extends T>... promises) {
        return provider.race(promises);
    }
    static  <T> JPromise<T> race(List<JPromise<? extends T>> promises) {
        return provider.race(promises);
    }
    static <T> JPromise<List<T>> all(List<JPromise<? extends T>> promises) {
        return provider.all(promises);
    }
    @SafeVarargs
    static  <T> JPromise<List<T>> all(JPromise<? extends T>... promises) {
        return provider.all(promises);
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

    static <T> JPromiseTrigger<T> createTrigger() {
        return provider.createTrigger();
    }

    static JPromise<JContext> context() {
        return JPromise.generate(Functions.PROMISE_HANDLER_EXTRACT_CONTEXT);
    }

    static JPromise<JContext> updateContext(JContext context) {
        return JPromise.generate((thunk, oldContext) -> {
            thunk.resolve(oldContext, context);
        });
    }

    static JPromise<JContext> updateContext(Function<JContext, JContext> contextUpdater) {
        return JPromise.generate((thunk, context) -> {
            JContext newContext = contextUpdater.apply(context);
            thunk.resolve(context, newContext);
        });
    }

    static <T> JPromise<T> wrapContext(JPromise<T> promise, JContext context) {
        return updateContext(context).thenImmediate(promise::withUpdateContext);
    }

    static <T> JPromise<T> wrapContext(JPromise<T> promise, Function<JContext, JContext> contextUpdater) {
        return updateContext(contextUpdater).thenImmediate(promise::withUpdateContext);
    }

    static <T> JPromise<T> wrapContext(JAsyncPromiseFunction0<JContext, T> function, JContext context) {
        return updateContext(context).thenWithContextImmediate((oldContext, newContext) -> function.apply(newContext).withUpdateContext(oldContext));
    }

    static <T> JPromise<T> wrapContext(JAsyncPromiseFunction0<JContext, T> function, Function<JContext, JContext> contextUpdater) {
        return updateContext(contextUpdater).thenWithContextImmediate((oldContext, newContext) -> function.apply(newContext).withUpdateContext(oldContext));
    }

    static <T> JPromise<T> wrapContext(JAsyncPromiseSupplier0<T> function, JContext context) {
        return updateContext(context).thenImmediate((oldContext) -> function.get().withUpdateContext(oldContext));
    }

    static <T> JPromise<T> wrapContext(JAsyncPromiseSupplier0<T> function, Function<JContext, JContext> contextUpdater) {
        return updateContext(contextUpdater).thenImmediate((oldContext) -> function.get().withUpdateContext(oldContext));
    }

    static JPromise<JScheduler> updateScheduler(JScheduler scheduler) {
        return JPromise.generate((thunk, context) -> {
            JScheduler oldScheduler = context.getScheduler();
            JContext newContext = context.setScheduler(scheduler);
            thunk.resolve(oldScheduler, newContext);
        });
    }

    static <T> JPromise<T> wrapScheduler(JPromise<T> promise, JScheduler scheduler) {
        return updateScheduler(scheduler).thenImmediate(promise::withUpdateScheduler);
    }

    static <T> JPromise<T> wrapScheduler(JAsyncPromiseSupplier0<T> function, JScheduler scheduler) {
        return updateScheduler(scheduler).thenImmediate((oldScheduler) -> function.get().withUpdateScheduler(oldScheduler));
    }

    static JPromise<Boolean> hasContextValue(Object key) {
        return JPromise.generate((thunk, context) -> {
            thunk.resolve(context.hasKey(key), context);
        });
    }

    static <T> JPromise<T> getContextValue(Object key) {
        return JPromise.generate((thunk, context) -> {
            thunk.resolve(context.get(key), context);
        });
    }

    static <T> JPromise<T> getContextValue(Object key, T defaultValue) {
        return JPromise.generate((thunk, context) -> {
            thunk.resolve(context.getOrDefault(key, defaultValue), context);
        });
    }

    static <T> JPromise<Optional<T>> getContextValueOrEmpty(Object key) {
        return JPromise.generate((thunk, context) -> {
            thunk.resolve(context.getOrEmpty(key), context);
        });
    }

    static JPromise<JScheduler> getScheduler() {
        return JPromise.generate(Functions.PROMISE_HANDLER_EXTRACT_SCHEDULER);
    }

    static <T> JPromise<T> setContextValue(Object key, Object newValue) {
        return JPromise.generate((thunk, context) -> {
            Object old = context.get(key);
            JContext newContext = context.set(key, newValue);
            //noinspection unchecked
            thunk.resolve((T) old, newContext);
        });
    }

    static JPromise<Void> updateContextValue(Object key, Function<Object, Object> valueUpdater) {
        return JPromise.generate((thunk, context) -> {
            Object value = context.get(key);
            JContext newContext = context.set(key, valueUpdater.apply(value));
            thunk.resolve(null, newContext);
        });
    }

    static <E> JPromise<Void> updateContextValue(Object key, Function<E, E> valueUpdater, E emptyValue) {
        return JPromise.generate((thunk, context) -> {
            if (context.hasKey(key)) {
                E value = context.get(key);
                JContext newContext = context.set(key, valueUpdater.apply(value));
                thunk.resolve(null, newContext);
            } else {
                JContext newContext = context.set(key, emptyValue);
                thunk.resolve(null, newContext);
            }
        });
    }

    static JPromise<Boolean> setContextValueIfExists(Object key, Object newValue) {
        return JPromise.generate((thunk, context) -> {
            if (context.hasKey(key)) {
                JContext newContext = context.set(key, newValue);
                thunk.resolve(true, newContext);
            } else {
                thunk.resolve(false, context);
            }
        });
    }

    static JPromise<Void> updateContextValueIfExists(Object key, Function<Object, Object> valueUpdater) {
        return JPromise.generate((thunk, context) -> {
            if (context.hasKey(key)) {
                Object value = context.get(key);
                JContext newContext = context.set(key, valueUpdater.apply(value));
                thunk.resolve(null, newContext);
            } else {
                thunk.resolve(null, context);
            }
        });
    }

    static <T> JPromise<T> removeContextValue(Object key) {
        return JPromise.generate((thunk, context) -> {
            Object old = context.get(key);
            JContext newContext = context.remove(key);
            //noinspection unchecked
            thunk.resolve((T) old, newContext);
        });
    }

    default T await() throws InterruptedException {
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

    default <R> JPromise<R> thenVoid() {
        return thenReturn(null);
    }

    default <R> JPromise<R> thenPromise(JPromise<R> nextPromise) {
        return thenImmediate(() -> nextPromise);
    }

    default JPromise<T> withContext(Consumer<JContext> consumer) {
        return thenWithWithContextImmediate((JContext context) -> {
            consumer.accept(context);
            return JPromise.empty();
        });
    }
    default JPromise<T> withUpdateContext(JContext context) {
        return thenWithImmediate(() -> updateContext(ctx -> context));
    }
    default JPromise<T> withUpdateContext(Function<JContext, JContext> contextUpdater) {
        return thenWithImmediate(() -> updateContext(contextUpdater));
    }
    default JPromise<T> withSetContextValue(Object key, Object value) {
        return thenWithImmediate(() -> setContextValue(key, value));
    }
    default JPromise<T> withRemoveContextValue(Object key) {
        return thenWithImmediate(() -> removeContextValue(key));
    }
    default JPromise<T> withUpdateContextValue(Object key, Function<Object, Object> updater, Object emptyValue) {
        return thenWithImmediate(() -> updateContextValue(key, updater, emptyValue));
    }
    default JPromise<T> withUpdateContextValueIfExists(Object key, Function<Object, Object> updater) {
        return thenWithImmediate(() -> updateContextValueIfExists(key, updater));
    }
    default JPromise<T> withUpdateScheduler(JScheduler scheduler) {
        return thenWithImmediate(() -> updateScheduler(scheduler));
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

    String MULTI_CATCH_ARGS_ERROR = "The arguments exceptionTypeAndCatches composed with throwable class and JPromiseCatchFunction0 or JPromiseCatchFunction1 pairs.";

    /**
     * <pre>
     * JAsyncCatchFunction1 closure1 = (e, ctx) -> { ... }
     * JAsyncCatchFunction0 closure2 = (e) -> { ... }
     * promise.doMultiCatches(
     *   immediate,
     *   IllegalArgumentException.class, closure1,
     *   RuntimeException.class, closure2
     * )
     * </pre>
     * equals to:
     * <pre>
     * promise.doCatch(immediate, (e, ctx) -> {
     *     if (e instanceof IllegalArgumentException) {
     *         return closure1.apply(e, ctx);
     *     } else if (e instanceof RuntimeException) {
     *         return closure2.apply(e);
     *     } else {
     *         throw e;
     *     }
     * })
     * </pre>
     *
     * @param immediate whether invoke the closure immediate ignore the current scheduler
     * @param exceptionTypeAndCatches exception type and catch closure pairs.
     *                                Both JAsyncCatchFunction0 and JAsyncCatchFunction1 are supported.
     * @return the promise
     */
    default JPromise<T> doMultiCatches(boolean immediate, Object... exceptionTypeAndCatches) {
        if ((exceptionTypeAndCatches.length & 1) == 1) {
            throw new IllegalArgumentException("The number of arguments exceptionTypeAndCatches must be even.");
        }
        for (int i = 0; i < exceptionTypeAndCatches.length;) {
            Object arg = exceptionTypeAndCatches[i++];
            if (!(arg instanceof Class)) {
                throw new IllegalArgumentException(MULTI_CATCH_ARGS_ERROR);
            }
            Class<?> exceptionType = (Class<?>) arg;
            if (!Throwable.class.isAssignableFrom(exceptionType)) {
                throw new IllegalArgumentException(MULTI_CATCH_ARGS_ERROR);
            }
            arg = exceptionTypeAndCatches[i++];
            if (!(arg instanceof JAsyncCatchFunction0) && !(arg instanceof JAsyncCatchFunction1)) {
                throw new IllegalArgumentException(MULTI_CATCH_ARGS_ERROR);
            }
        }
        return doCatchWithContext((e, ctx) -> {
            for (int i = 0; i < exceptionTypeAndCatches.length;) {
                Class<?> exceptionType = (Class<?>) exceptionTypeAndCatches[i++];
                Object closure = exceptionTypeAndCatches[i++];
                if (exceptionType.isInstance(e)) {
                    if (closure instanceof JAsyncCatchFunction0) {
                        //noinspection unchecked
                        return ((JAsyncCatchFunction0<Throwable, T>) closure).apply(e);
                    } else {
                        //noinspection unchecked
                        return ((JAsyncCatchFunction1<Throwable, T>) closure).apply(e, ctx);
                    }
                }
            }
            throw e;
        }, immediate);
    }

    /**
     * same as doMultiCatches(false, exceptionTypeAndCatches)
     * @param exceptionTypeAndCatches exception type and catch closure pairs.
     *                                Both JAsyncCatchFunction0 and JAsyncCatchFunction1 are supported.
     * @return the promise
     * @see #doMultiCatches(boolean, Object...)
     */
    default JPromise<T> doMultiCatches(Object... exceptionTypeAndCatches) {
        return doMultiCatches(false, exceptionTypeAndCatches);
    }

    /**
     * same as doMultiCatches(true, exceptionTypeAndCatches)
     * @param exceptionTypeAndCatches exception type and catch closure pairs.
     *                                Both JAsyncCatchFunction0 and JAsyncCatchFunction1 are supported.
     * @return the promise
     * @see #doMultiCatches(boolean, Object...)
     */
    default JPromise<T> doMultiCatchImmediate(Object... exceptionTypeAndCatches) {
        return doMultiCatches(true, exceptionTypeAndCatches);
    }

    <R> JPromise<R> thenOrCatchWithContext(JAsyncPromiseFunction3<T, R> handler, boolean immediate);
    default <R> JPromise<R> thenOrCatchWithContext(JAsyncPromiseFunction3<T, R> handler) {
        return thenOrCatchWithContext(handler, false);
    }
    default <R> JPromise<R> thenOrCatchWithContextImmediate(JAsyncPromiseFunction3<T, R> handler) {
        return thenOrCatchWithContext(handler, true);
    }
    default <R> JPromise<R> thenOrCatch(JAsyncPromiseFunction2<T, R> handler) {
        return thenOrCatchWithContext((t, throwable, context) -> handler.apply(t, throwable));
    }
    default <R> JPromise<R> thenOrCatchImmediate(JAsyncPromiseFunction2<T, R> handler) {
        return thenOrCatchWithContext((t, throwable, context) -> handler.apply(t, throwable), true);
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

    JPromise<T> onSuccess(BiConsumer<T, JContext> resolver);
    default JPromise<T> onSuccess(Consumer<T> consumer) {
        return onSuccess((v, ctx) -> consumer.accept(v));
    }
    JPromise<T> onError(BiConsumer<Throwable, JContext> reject);
    default JPromise<T> onError(Consumer<Throwable> consumer) {
        return onError((error, ctx) -> consumer.accept(error));
    }
    JPromise<T> onFinally(TriConsumer<T, Throwable, JContext> consumer);
    default JPromise<T> onFinally(Runnable runnable) {
        return onFinally((v, e, ctx) -> runnable.run());
    }
    default JPromise<T> onFinally(Consumer<JContext> runnable) {
        return onFinally((v, e, ctx) -> runnable.accept(ctx));
    }
    default JPromise<T> onFinally(BiConsumer<T, Throwable> runnable) {
        return onFinally((v, e, ctx) -> runnable.accept(v, e));
    }

    void schedule(JContext context);

    JHandle<T> async(JContext context);
    default JHandle<T> async() {
        return async(JContext.defaultContext());
    }
}
