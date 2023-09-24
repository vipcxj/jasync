package io.github.vipcxj.jasync.ng.spec;

import io.github.vipcxj.jasync.ng.spec.functional.*;
import io.github.vipcxj.jasync.ng.spec.spi.JPromiseSupport;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings({"unchecked", "unused"})
public interface JPromise<T> extends JHandle<T>, CompletionStage<T> {
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
    static JPromise<Void> sleep(Duration timeout) {
        if (timeout == null) {
            return JPromise.empty();
        }
        return provider.sleep(timeout.toNanos(), TimeUnit.NANOSECONDS);
    }
    static <T> JPromise<T> timeout(JPromise<T> promise, T whenTimeout, long timeout, TimeUnit unit) {
        if (timeout <= 0) {
            return promise;
        } else {
            return JPromise.any(
                    promise,
                    JPromise.sleep(timeout, unit).thenReturn(whenTimeout)
            );
        }
    }
    static <T> JPromise<T> timeout(JPromise<T> promise, T whenTimeout, Duration timeout) {
        if (timeout == null) {
            return promise;
        }
        return timeout(promise, whenTimeout, timeout.toNanos(), TimeUnit.NANOSECONDS);
    }
    static <T> JPromise<T> timeout(JPromise<T> promise, long timeout, TimeUnit unit) {
        if (timeout <= 0) {
            return promise;
        } else {
            return JPromise.any(
                    promise,
                    JPromise.sleep(timeout, unit).thenThrow(new TimeoutException())
            );
        }
    }
    static <T> JPromise<T> timeout(JPromise<T> promise, Duration timeout) {
        if (timeout == null) {
            return promise;
        }
        return timeout(promise, timeout.toNanos(), TimeUnit.NANOSECONDS);
    }
    static <T> JPromise<T> from(CompletionStage<T> stage) {
        if (stage instanceof JPromise) {
            return (JPromise<T>) stage;
        } else {
            return JPromise.generate((thunk, context) -> {
                stage.whenComplete((value, exception) -> {
                    if (exception == null) {
                        thunk.resolve(value, context);
                    } else {
                        if (exception instanceof CompletionException) {
                            thunk.reject(exception.getCause(), context);
                        } else {
                            thunk.reject(exception, context);
                        }
                    }
                });
            });
        }
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
    default T await() {
        throw new UnsupportedOperationException("The method \"await\" should be called in an async method.");
    }
    default T await(AwaitType type) {
        throw new UnsupportedOperationException("The method \"await\" should be called in an async method.");
    }
    @SuppressWarnings("RedundantThrows")
    default T awaitInterruptable() throws InterruptedException {
        throw new UnsupportedOperationException("The method \"awaitInterruptable\" should be called in an async method.");
    }
    @SuppressWarnings("RedundantThrows")
    default T awaitInterruptable(AwaitType type) throws InterruptedException {
        throw new UnsupportedOperationException("The method \"awaitInterruptable\" should be called in an async method.");
    }
    <R> JPromise<R> thenWithContext(AwaitType type, JAsyncPromiseFunction1<T, R> mapper);
    default <R> JPromise<R> thenWithContext(JAsyncPromiseFunction1<T, R> mapper) {
        return thenWithContext(AwaitType.AUTO, mapper);
    }
    default <R> JPromise<R> thenWithContextImmediate(JAsyncPromiseFunction1<T, R> mapper) {
        return thenWithContext(AwaitType.IMMEDIATE, mapper);
    }

    default <R> JPromise<R> then(AwaitType type, JAsyncPromiseFunction0<T, R> mapper) {
        return thenWithContext(type, (v, ctx) -> mapper.apply(v));
    }
    default <R> JPromise<R> then(JAsyncPromiseFunction0<T, R> mapper) {
        return then(AwaitType.AUTO, mapper);
    }
    default <R> JPromise<R> thenImmediate(JAsyncPromiseFunction0<T, R> mapper) {
        return then(AwaitType.IMMEDIATE, mapper);
    }

    default <R> JPromise<R> thenWithContext(AwaitType type, JAsyncPromiseSupplier1<R> supplier) {
        return thenWithContext(type, (ignored, ctx) -> supplier.get(ctx));
    }
    default <R> JPromise<R> thenWithContext(JAsyncPromiseSupplier1<R> supplier) {
        return thenWithContext(AwaitType.AUTO, supplier);
    }
    default <R> JPromise<R> thenWithContextImmediate(JAsyncPromiseSupplier1<R> supplier) {
        return thenWithContext(AwaitType.IMMEDIATE, supplier);
    }

    default <R> JPromise<R> then(AwaitType type, JAsyncPromiseSupplier0<R> supplier) {
        return then(type, (T ignored) -> supplier.get());
    }
    default <R> JPromise<R> then(JAsyncPromiseSupplier0<R> supplier) {
        return then(AwaitType.AUTO, supplier);
    }
    default <R> JPromise<R> thenImmediate(JAsyncPromiseSupplier0<R> supplier) {
        return then(AwaitType.IMMEDIATE, supplier);
    }

    default <R> JPromise<R> thenMapWithContext(AwaitType type, JAsyncFunction1<T, R> function) {
        return thenWithContext(type, (T v, JContext ctx) -> JPromise.just(function.apply(v, ctx)));
    }
    default <R> JPromise<R> thenMapWithContext(JAsyncFunction1<T, R> function) {
        return thenMapWithContext(AwaitType.AUTO, function);
    }
    default <R> JPromise<R> thenMapWithContextImmediate(JAsyncFunction1<T, R> function) {
        return thenMapWithContext(AwaitType.IMMEDIATE, function);
    }

    default <R> JPromise<R> thenMap(AwaitType type, JAsyncFunction0<T, R> function) {
        return then(type, (T v) -> JPromise.just(function.apply(v)));
    }
    default <R> JPromise<R> thenMap(JAsyncFunction0<T, R> function) {
        return thenMap(AwaitType.AUTO, function);
    }
    default <R> JPromise<R> thenMapImmediate(JAsyncFunction0<T, R> function) {
        return thenMap(AwaitType.IMMEDIATE, function);
    }

    default <R> JPromise<R> thenMapWithContext(AwaitType type, JAsyncSupplier1<R> function) {
        return thenWithContext(type, (v, ctx) -> JPromise.just(function.get(ctx)));
    }
    default <R> JPromise<R> thenMapWithContext(JAsyncSupplier1<R> function) {
        return thenMapWithContext(AwaitType.AUTO, function);
    }
    default <R> JPromise<R> thenMapWithContextImmediate(JAsyncSupplier1<R> function) {
        return thenMapWithContext(AwaitType.IMMEDIATE, function);
    }

    default <R> JPromise<R> thenMap(AwaitType type, JAsyncSupplier0<R> function) {
        return then(type, (T v) -> JPromise.just(function.get()));
    }
    default <R> JPromise<R> thenMap(JAsyncSupplier0<R> function) {
        return thenMap(AwaitType.AUTO, function);
    }
    default <R> JPromise<R> thenMapImmediate(JAsyncSupplier0<R> function) {
        return thenMap(AwaitType.IMMEDIATE, function);
    }

    default <R> JPromise<T> thenWithWithContext(AwaitType type, JAsyncPromiseFunction1<T, R> function) {
        return thenWithContext(type, (v, ctx) -> function.apply(v, ctx).thenReturn(v));
    }
    default <R> JPromise<T> thenWithWithContext(JAsyncPromiseFunction1<T, R> function) {
        return thenWithWithContext(AwaitType.AUTO, function);
    }
    default <R> JPromise<T> thenWithWithContextImmediate(JAsyncPromiseFunction1<T, R> function) {
        return thenWithWithContext(AwaitType.IMMEDIATE, function);
    }

    default <R> JPromise<T> thenWith(AwaitType type, JAsyncPromiseFunction0<T, R> function) {
        return then(type, v -> function.apply(v).thenReturn(v));
    }
    default <R> JPromise<T> thenWith(JAsyncPromiseFunction0<T, R> function) {
        return thenWith(AwaitType.AUTO, function);
    }
    default <R> JPromise<T> thenWithImmediate(JAsyncPromiseFunction0<T, R> function) {
        return thenWith(AwaitType.IMMEDIATE, function);
    }

    default <R> JPromise<T> thenWithWithContext(AwaitType type, JAsyncPromiseSupplier1<R> supplier) {
        return thenWithContext(type, (v, ctx) -> supplier.get(ctx).thenReturn(v));
    }
    default <R> JPromise<T> thenWithWithContext(JAsyncPromiseSupplier1<R> supplier) {
        return thenWithWithContext(AwaitType.AUTO, supplier);
    }
    default <R> JPromise<T> thenWithWithContextImmediate(JAsyncPromiseSupplier1<R> supplier) {
        return thenWithWithContext(AwaitType.IMMEDIATE, supplier);
    }

    default <R> JPromise<T> thenWith(AwaitType type, JAsyncPromiseSupplier0<R> supplier) {
        return then(type, v -> supplier.get().thenReturn(v));
    }
    default <R> JPromise<T> thenWith(JAsyncPromiseSupplier0<R> supplier) {
        return thenWith(AwaitType.AUTO, supplier);
    }
    default <R> JPromise<T> thenWithImmediate(JAsyncPromiseSupplier0<R> supplier) {
        return thenWith(AwaitType.IMMEDIATE, supplier);
    }

    default <R> JPromise<R> thenReturn(R next) {
        return thenMapImmediate(() -> next);
    }

    default <R> JPromise<R> thenVoid() {
        return thenReturn(null);
    }

    default <R> JPromise<R> thenThrow(Throwable t) {
        return  thenPromise(JPromise.error(t));
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

    JPromise<T> doCatchWithContext(AwaitType type, JAsyncCatchFunction1<Throwable, T> catcher);
    default JPromise<T> doCatchWithContext(JAsyncCatchFunction1<Throwable, T> catcher) {
        return doCatchWithContext(AwaitType.AUTO, catcher);
    }
    default JPromise<T> doCatchWithContextImmediate(JAsyncCatchFunction1<Throwable, T> catcher) {
        return doCatchWithContext(AwaitType.IMMEDIATE, catcher);
    }

    default JPromise<T> doCatch(AwaitType type, JAsyncCatchFunction0<Throwable, T> catcher) {
        return doCatchWithContext(type, (error, ctx) -> catcher.apply(error));
    }
    default JPromise<T> doCatch(JAsyncCatchFunction0<Throwable, T> catcher) {
        return doCatch(AwaitType.AUTO, catcher);
    }
    default JPromise<T> doCatchImmediate(JAsyncCatchFunction0<Throwable, T> catcher) {
        return doCatch(AwaitType.IMMEDIATE, catcher);
    }

    String MULTI_CATCH_ARGS_ERROR = "The arguments exceptionTypeAndCatches composed with throwable class and JPromiseCatchFunction0 or JPromiseCatchFunction1 pairs.";

    /**
     * <pre>
     * JAsyncCatchFunction1 closure1 = (e, ctx) -> { ... }
     * JAsyncCatchFunction0 closure2 = (e) -> { ... }
     * promise.doMultiCatches(
     *   type,
     *   IllegalArgumentException.class, closure1,
     *   RuntimeException.class, closure2
     * )
     * </pre>
     * equals to:
     * <pre>
     * promise.doCatch(type, (e, ctx) -> {
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
     * @param type whether invoke the closure immediate ignore the current scheduler or using scheduler or auto detection by the framework.
     * @param exceptionTypeAndCatches exception type and catch closure pairs.
     *                                Both JAsyncCatchFunction0 and JAsyncCatchFunction1 are supported.
     * @return the promise
     */
    default JPromise<T> doMultiCatches(AwaitType type, Object... exceptionTypeAndCatches) {
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
        return doCatchWithContext(type, (e, ctx) -> {
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
        });
    }

    /**
     * same as doMultiCatches(false, exceptionTypeAndCatches)
     * @param exceptionTypeAndCatches exception type and catch closure pairs.
     *                                Both JAsyncCatchFunction0 and JAsyncCatchFunction1 are supported.
     * @return the promise
     * @see #doMultiCatches(AwaitType, Object...)
     */
    default JPromise<T> doMultiCatches(Object... exceptionTypeAndCatches) {
        return doMultiCatches(AwaitType.AUTO, exceptionTypeAndCatches);
    }

    /**
     * same as doMultiCatches(true, exceptionTypeAndCatches)
     * @param exceptionTypeAndCatches exception type and catch closure pairs.
     *                                Both JAsyncCatchFunction0 and JAsyncCatchFunction1 are supported.
     * @return the promise
     * @see #doMultiCatches(AwaitType, Object...)
     */
    default JPromise<T> doMultiCatchImmediate(Object... exceptionTypeAndCatches) {
        return doMultiCatches(AwaitType.IMMEDIATE, exceptionTypeAndCatches);
    }

    <R> JPromise<R> thenOrCatchWithContext(AwaitType type, JAsyncPromiseFunction3<T, R> handler);
    default <R> JPromise<R> thenOrCatchWithContext(JAsyncPromiseFunction3<T, R> handler) {
        return thenOrCatchWithContext(AwaitType.AUTO, handler);
    }
    default <R> JPromise<R> thenOrCatchWithContextImmediate(JAsyncPromiseFunction3<T, R> handler) {
        return thenOrCatchWithContext(AwaitType.IMMEDIATE, handler);
    }
    default <R> JPromise<R> thenOrCatch(JAsyncPromiseFunction2<T, R> handler) {
        return thenOrCatchWithContext((t, throwable, context) -> handler.apply(t, throwable));
    }
    default <R> JPromise<R> thenOrCatchImmediate(JAsyncPromiseFunction2<T, R> handler) {
        return thenOrCatchWithContext(AwaitType.IMMEDIATE, (t, throwable, context) -> handler.apply(t, throwable));
    }

    <R> JPromise<T> doFinallyWithContext(AwaitType type, JAsyncPromiseSupplier1<R> supplier);
    default <R> JPromise<T> doFinallyWithContext(JAsyncPromiseSupplier1<R> supplier) {
        return doFinallyWithContext(AwaitType.AUTO, supplier);
    }
    default <R> JPromise<T> doFinallyWithContextImmediate(JAsyncPromiseSupplier1<R> supplier) {
        return doFinallyWithContext(AwaitType.IMMEDIATE, supplier);
    }

    default <R> JPromise<T> doFinally(AwaitType type, JAsyncPromiseSupplier0<R> supplier) {
        return doFinallyWithContext(type, ctx -> supplier.get());
    }
    default <R> JPromise<T> doFinally(JAsyncPromiseSupplier0<R> supplier) {
        return doFinally(AwaitType.AUTO, supplier);
    }
    default <R> JPromise<T> doFinallyImmediate(JAsyncPromiseSupplier0<R> supplier) {
        return doFinally(AwaitType.IMMEDIATE, supplier);
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


    @Override
    default <U> JPromise<U> thenApply(Function<? super T,? extends U> fn) {
        return thenMapImmediate(fn::apply);
    }

    @Override
    default <U> JPromise<U> thenApplyAsync(Function<? super T,? extends U> fn) {
        return thenMap(fn::apply);
    }

    @Override
    default <U> JPromise<U> thenApplyAsync(Function<? super T,? extends U> fn, Executor executor) {
        if (executor == null) {
            return thenApplyAsync(fn);
        } else {
            JScheduler scheduler = JScheduler.fromExecutorService(executor);
            return wrapContext(thenApplyAsync(fn), ctx -> ctx.setScheduler(scheduler));
        }
    }

    @Override
    default JPromise<Void> thenAccept(Consumer<? super T> action) {
        return thenMapImmediate(v -> {
            action.accept(v);
            return null;
        });
    }

    @Override
    default JPromise<Void> thenAcceptAsync(Consumer<? super T> action) {
        return thenMap(v -> {
            action.accept(v);
            return null;
        });
    }

    @Override
    default JPromise<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
        if (executor == null) {
            return thenAcceptAsync(action);
        } else {
            JScheduler scheduler = JScheduler.fromExecutorService(executor);
            return wrapContext(thenAcceptAsync(action), ctx -> ctx.setScheduler(scheduler));
        }
    }

    @Override
    default JPromise<Void> thenRun(Runnable action) {
        return thenImmediate(() -> {
            action.run();
            return JPromise.empty();
        });
    }

    @Override
    default JPromise<Void> thenRunAsync(Runnable action) {
        return then(() -> {
            action.run();
            return JPromise.empty();
        });
    }

    @Override
    default JPromise<Void> thenRunAsync(Runnable action, Executor executor) {
        if (executor == null) {
            return thenRunAsync(action);
        } else {
            JScheduler scheduler = JScheduler.fromExecutorService(executor);
            return wrapContext(thenRunAsync(action), ctx -> ctx.setScheduler(scheduler));
        }
    }

    @Override
    default <U,V> JPromise<V> thenCombine(CompletionStage<? extends U> other, BiFunction<? super T,? super U,? extends V> fn) {
        return thenImmediate(v1 -> {
            return JPromise.from(other).thenMapImmediate(v2 -> {
                return fn.apply(v1, v2);
            });
        });
    }

    @Override
    default <U,V> JPromise<V> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T,? super U,? extends V> fn) {
        return thenImmediate(v1 -> {
            return JPromise.from(other).thenMap(v2 -> {
                return fn.apply(v1, v2);
            });
        });
    }

    @Override
    default <U,V> JPromise<V> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T,? super U,? extends V> fn, Executor executor) {
        if (executor == null) {
            return thenCombineAsync(other, fn);
        } else {
            JScheduler scheduler = JScheduler.fromExecutorService(executor);
            return wrapContext(thenCombineAsync(other, fn), ctx -> ctx.setScheduler(scheduler));
        }
    }

    @Override
    default <U> JPromise<Void> thenAcceptBoth(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        return thenImmediate(v1 -> {
            return JPromise.from(other).thenImmediate(v2 -> {
                action.accept(v1, v2);
                return JPromise.empty();
            });
        });
    }

    @Override
    default <U> JPromise<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        return thenImmediate(v1 -> {
            return JPromise.from(other).then(v2 -> {
                action.accept(v1, v2);
                return JPromise.empty();
            });
        });
    }

    @Override
    default <U> JPromise<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action, Executor executor) {
        if (executor == null) {
            return thenAcceptBothAsync(other, action);
        } else {
            JScheduler scheduler = JScheduler.fromExecutorService(executor);
            return wrapContext(thenAcceptBothAsync(other, action), ctx -> ctx.setScheduler(scheduler));
        }
    }

    @Override
    default JPromise<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        return thenImmediate(() -> {
            return JPromise.from(other).thenImmediate(() -> {
                action.run();
                return JPromise.empty();
            });
        });
    }

    @Override
    default JPromise<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        return thenImmediate(() -> {
            return JPromise.from(other).then(() -> {
                action.run();
                return JPromise.empty();
            });
        });
    }

    @Override
    default JPromise<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        if (executor == null) {
            return runAfterBothAsync(other, action);
        } else {
            JScheduler scheduler = JScheduler.fromExecutorService(executor);
            return wrapContext(runAfterBothAsync(other, action), ctx -> ctx.setScheduler(scheduler));
        }
    }

    @Override
    default <U> JPromise<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return JPromise.any(this, from(other)).thenMapImmediate(fn::apply);
    }

    @Override
    default <U> JPromise<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return JPromise.any(this, from(other)).thenMap(fn::apply);
    }

    @Override
    default <U> JPromise<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn, Executor executor) {
        if (executor == null) {
            return applyToEitherAsync(other, fn);
        } else {
            JScheduler scheduler = JScheduler.fromExecutorService(executor);
            return wrapContext(applyToEitherAsync(other, fn), ctx -> ctx.setScheduler(scheduler));
        }
    }

    @Override
    default JPromise<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action) {
        return JPromise.any(this, from(other)).thenImmediate(v -> {
            action.accept(v);
            return JPromise.empty();
        });
    }

    @Override
    default JPromise<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action) {
        return JPromise.any(this, from(other)).then(v -> {
            action.accept(v);
            return JPromise.empty();
        });
    }

    @Override
    default JPromise<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action, Executor executor) {
        if (executor == null) {
            return acceptEitherAsync(other, action);
        } else {
            JScheduler scheduler = JScheduler.fromExecutorService(executor);
            return wrapContext(acceptEitherAsync(other, action), ctx -> ctx.setScheduler(scheduler));
        }
    }

    @Override
    default JPromise<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
        return JPromise.any(this, from(other)).thenImmediate(() -> {
            action.run();
            return JPromise.empty();
        });
    }

    @Override
    default JPromise<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
       return JPromise.any(this, from(other)).then(() -> {
            action.run();
            return JPromise.empty();
        });
    }

    @Override
    default JPromise<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        if (executor == null) {
            return runAfterEitherAsync(other, action);
        } else {
            JScheduler scheduler = JScheduler.fromExecutorService(executor);
            return wrapContext(runAfterEitherAsync(other, action), ctx -> ctx.setScheduler(scheduler));
        }
    }

    @Override
    default <U> JPromise<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
        return thenImmediate(v -> JPromise.from(fn.apply(v)));
    }

    @Override
    default <U> JPromise<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
        return then(v -> JPromise.from(fn.apply(v)));
    }

    @Override
    default <U> JPromise<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn, Executor executor) {
        if (executor == null) {
            return thenComposeAsync(fn);
        } else {
            JScheduler scheduler = JScheduler.fromExecutorService(executor);
            return wrapContext(thenComposeAsync(fn), ctx -> ctx.setScheduler(scheduler));
        }
    }

    @Override    
    default <U> JPromise<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
        return thenOrCatchImmediate((v, e) -> {
            return JPromise.just(fn.apply(v, e));
        });
    }

    @Override
    default <U> JPromise<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
        return thenOrCatch((v, e) -> {
            return JPromise.just(fn.apply(v, e));
        });
    }

    @Override
    default <U> JPromise<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
        if (executor == null) {
            return handleAsync(fn);
        } else {
            JScheduler scheduler = JScheduler.fromExecutorService(executor);
            return wrapContext(handleAsync(fn), ctx -> ctx.setScheduler(scheduler));
        }
    }

    @Override
    default JPromise<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        return thenOrCatchImmediate((v, e) -> {
            try {
                action.accept(v, e);
            } catch (Throwable e1) {
                if (e != null) {
                    return JPromise.error(e);
                } else {
                    return JPromise.error(e1);
                }
            }
            if (e != null) {
                return JPromise.error(e);
            } else {
                return JPromise.just(v);
            }
        });
    }

    @Override
    default JPromise<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
        return thenOrCatch((v, e) -> {
            try {
                action.accept(v, e);
            } catch (Throwable e1) {
                if (e != null) {
                    return JPromise.error(e);
                } else {
                    return JPromise.error(e1);
                }
            }
            if (e != null) {
                return JPromise.error(e);
            } else {
                return JPromise.just(v);
            }
        });
    }

    @Override
    default JPromise<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor) {
        if (executor == null) {
            return whenCompleteAsync(action);
        } else {
            JScheduler scheduler = JScheduler.fromExecutorService(executor);
            return wrapContext(whenCompleteAsync(action), ctx -> ctx.setScheduler(scheduler));
        }
    }

    @Override
    default JPromise<T> exceptionally(Function<Throwable, ? extends T> fn) {
        return doCatchImmediate(e -> {
            return JPromise.just(fn.apply(e));
        });
    }

    @Override
    default CompletableFuture<T> toCompletableFuture() {
        if (this instanceof CompletableFuture) {
            return (CompletableFuture<T>) this;
        } else {
            CompletableFuture<T> future = new CompletableFuture<>();
            onFinally((v, e) -> {
                if (e != null) {
                    future.completeExceptionally(e);
                } else {
                    future.complete(v);
                }
            });
            return future;
        }
    }
}
