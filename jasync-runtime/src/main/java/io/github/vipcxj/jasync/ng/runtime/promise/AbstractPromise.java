package io.github.vipcxj.jasync.ng.runtime.promise;

import io.github.vipcxj.jasync.ng.runtime.schedule.ImmediateTask;
import io.github.vipcxj.jasync.ng.runtime.schedule.LazyTask;
import io.github.vipcxj.jasync.ng.runtime.utils.UnPaddedLockFreeArrayQueue0;
import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JHandle;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JThunk;
import io.github.vipcxj.jasync.ng.spec.exceptions.*;
import io.github.vipcxj.jasync.ng.spec.functional.*;

import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BiConsumer;

public abstract class AbstractPromise<T> implements JPromise<T>, JThunk<T> {

    protected T value;
    protected Throwable error;
    protected volatile int state;
    @SuppressWarnings("rawtypes")
    private static final AtomicIntegerFieldUpdater<AbstractPromise> STATE = AtomicIntegerFieldUpdater.newUpdater(AbstractPromise.class, "state");
    protected static final int ST_INIT = 0;
    protected static final int ST_INIT_READ = 1;
    protected static final int ST_INIT_TERMING = 2;
    protected static final int ST_RUNNING = 3;
    protected static final int ST_RUNNING_READ = 4;
    protected static final int ST_RUNNING_TERMING = 5;
    protected static final int ST_RUNNING_WRITE = 6;
    protected static final int ST_COMPLETING = 7;
    protected static final int ST_RESOLVING = 8;
    protected static final int ST_RESOLVING_BUSY = 9;
    protected static final int ST_REJECTING = 10;
    protected static final int ST_REJECTING_BUSY = 11;
    protected static final int ST_UNCOMPLETED = 12;
    protected static final int ST_RESOLVED = 13;
    protected static final int ST_REJECTED = 14;

    private final AbstractPromise<?> parent;
    private volatile Object children;
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<AbstractPromise, Object> CHILDREN = AtomicReferenceFieldUpdater.newUpdater(AbstractPromise.class, Object.class, "children");
    private volatile Object successCallbacks;
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<AbstractPromise, Object> SUCCESS_CALLBACKS = AtomicReferenceFieldUpdater.newUpdater(AbstractPromise.class, Object.class, "successCallbacks");
    private volatile Object errorCallbacks;
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<AbstractPromise, Object> ERROR_CALLBACKS = AtomicReferenceFieldUpdater.newUpdater(AbstractPromise.class, Object.class, "errorCallbacks");
    private volatile Object canceledCallbacks;
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<AbstractPromise, Object> CANCELED_CALLBACKS = AtomicReferenceFieldUpdater.newUpdater(AbstractPromise.class, Object.class, "canceledCallbacks");
    private volatile Object finallyCallbacks;
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<AbstractPromise, Object> FINALLY_CALLBACKS = AtomicReferenceFieldUpdater.newUpdater(AbstractPromise.class, Object.class, "finallyCallbacks");
    private volatile Object requestCancelCallback;
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<AbstractPromise, Object> REQUEST_CANCEL_CALLBACK = AtomicReferenceFieldUpdater.newUpdater(AbstractPromise.class, Object.class, "requestCancelCallback");


    public AbstractPromise(AbstractPromise<?> parent) {
        this.parent = parent;
    }

    public static <T> GeneralPromise<T> generate(BiConsumer<JThunk<T>, JContext> handler, AbstractPromise<?> parent) {
        return new GeneralPromise<>(new ImmediateTask<>(handler), parent);
    }

    public static <T> GeneralPromise<T> create(BiConsumer<JThunk<T>, JContext> handler, long delay, TimeUnit timeUnit, AbstractPromise<?> parent) {
        return new GeneralPromise<>(new LazyTask<>(handler, delay, timeUnit), parent);
    }

    public static <T> GeneralPromise<T> create(BiConsumer<JThunk<T>, JContext> handler, AbstractPromise<?> parent) {
        return new GeneralPromise<>(new LazyTask<>(handler), parent);
    }

    protected abstract void run(JContext context);


    protected void offerChild(JPromise<?> child) {
        while (true) {
            Object children = this.children;
            if (children instanceof JPromise) {
                Queue<JPromise<?>> childrenQueue = new UnPaddedLockFreeArrayQueue0<>(2);
                if (this.children == children && CHILDREN.weakCompareAndSet(this, children, childrenQueue)) {
                    childrenQueue.offer((JPromise<?>) children);
                    childrenQueue.offer(child);
                    return;
                }
            } else if (children instanceof Queue) {
                //noinspection unchecked
                Queue<JPromise<?>> childrenQueue = (Queue<JPromise<?>>) children;
                childrenQueue.offer(child);
                return;
            } else if (children == null && CHILDREN.weakCompareAndSet(this, null, child)) {
                return;
            }
        }
    }

    protected JPromise<?> pollChildren() {
        while (true) {
            Object children = this.children;
            if (children == null) {
                return null;
            } else if (children instanceof JPromise && CHILDREN.weakCompareAndSet(this, children ,null)) {
                return (JPromise<?>) children;
            } else if (children instanceof Queue) {
                //noinspection unchecked
                Queue<JPromise<?>> childrenQueue = (Queue<JPromise<?>>) children;
                return childrenQueue.poll();
            }
        }
    }

    protected void offerSuccessCallbacks(BiConsumer<T, JContext> callback) {
        while (true) {
            Object successCallbacks = this.successCallbacks;
            if (successCallbacks instanceof BiConsumer) {
                Queue<BiConsumer<T, JContext>> callbacks = new UnPaddedLockFreeArrayQueue0<>(2);
                if (this.successCallbacks == successCallbacks && SUCCESS_CALLBACKS.weakCompareAndSet(this, successCallbacks, callbacks)) {
                    //noinspection unchecked
                    callbacks.offer((BiConsumer<T, JContext>) successCallbacks);
                    callbacks.offer(callback);
                    return;
                }
            } else if (successCallbacks instanceof Queue) {
                //noinspection unchecked
                Queue<BiConsumer<T, JContext>> callbacks = (Queue<BiConsumer<T, JContext>>) successCallbacks;
                callbacks.offer(callback);
                return;
            } else if (successCallbacks == null && SUCCESS_CALLBACKS.weakCompareAndSet(this, null, callback)) {
                return;
            }
        }
    }

    protected BiConsumer<T, JContext> pollSuccessCallback() {
        while (true) {
            Object successCallbacks = this.successCallbacks;
            if (successCallbacks == null) {
                return null;
            } else if (successCallbacks instanceof BiConsumer && SUCCESS_CALLBACKS.weakCompareAndSet(this, successCallbacks ,null)) {
                //noinspection unchecked
                return (BiConsumer<T, JContext>) successCallbacks;
            } else if (successCallbacks instanceof Queue) {
                //noinspection unchecked
                Queue<BiConsumer<T, JContext>> callbacks = (Queue<BiConsumer<T, JContext>>) successCallbacks;
                return callbacks.poll();
            }
        }
    }

    protected void offerErrorCallbacks(BiConsumer<Throwable, JContext> callback) {
        while (true) {
            Object errorCallbacks = this.errorCallbacks;
            if (errorCallbacks instanceof BiConsumer) {
                Queue<BiConsumer<Throwable, JContext>> callbacks = new UnPaddedLockFreeArrayQueue0<>(2);
                if (this.errorCallbacks == errorCallbacks && ERROR_CALLBACKS.weakCompareAndSet(this, errorCallbacks, callbacks)) {
                    //noinspection unchecked
                    callbacks.offer((BiConsumer<Throwable, JContext>) errorCallbacks);
                    callbacks.offer(callback);
                    return;
                }
            } else if (errorCallbacks instanceof Queue) {
                //noinspection unchecked
                Queue<BiConsumer<Throwable, JContext>> callbacks = (Queue<BiConsumer<Throwable, JContext>>) errorCallbacks;
                callbacks.offer(callback);
                return;
            } else if (errorCallbacks == null && ERROR_CALLBACKS.weakCompareAndSet(this, null, callback)) {
                return;
            }
        }
    }

    protected BiConsumer<Throwable, JContext> pollErrorCallback() {
        while (true) {
            Object errorCallbacks = this.errorCallbacks;
            if (errorCallbacks == null) {
                return null;
            } else if (errorCallbacks instanceof BiConsumer && ERROR_CALLBACKS.weakCompareAndSet(this, errorCallbacks ,null)) {
                //noinspection unchecked
                return (BiConsumer<Throwable, JContext>) errorCallbacks;
            } else if (errorCallbacks instanceof Queue) {
                //noinspection unchecked
                Queue<BiConsumer<Throwable, JContext>> callbacks = (Queue<BiConsumer<Throwable, JContext>>) errorCallbacks;
                return callbacks.poll();
            }
        }
    }

    protected void offerCanceledCallbacks(BiConsumer<InterruptedException, JContext> callback) {
        while (true) {
            Object canceledCallbacks = this.canceledCallbacks;
            if (canceledCallbacks instanceof BiConsumer) {
                Queue<BiConsumer<InterruptedException, JContext>> callbacks = new UnPaddedLockFreeArrayQueue0<>(2);
                if (this.canceledCallbacks == canceledCallbacks && CANCELED_CALLBACKS.weakCompareAndSet(this, canceledCallbacks, callbacks)) {
                    //noinspection unchecked
                    callbacks.offer((BiConsumer<InterruptedException, JContext>) canceledCallbacks);
                    callbacks.offer(callback);
                    return;
                }
            } else if (canceledCallbacks instanceof Queue) {
                //noinspection unchecked
                Queue<BiConsumer<InterruptedException, JContext>> callbacks = (Queue<BiConsumer<InterruptedException, JContext>>) canceledCallbacks;
                callbacks.offer(callback);
                return;
            } else if (canceledCallbacks == null && CANCELED_CALLBACKS.weakCompareAndSet(this, null, callback)) {
                return;
            }
        }
    }

    protected BiConsumer<InterruptedException, JContext> pollCancelCallback() {
        while (true) {
            Object canceledCallbacks = this.canceledCallbacks;
            if (canceledCallbacks == null) {
                return null;
            } else if (canceledCallbacks instanceof BiConsumer && CANCELED_CALLBACKS.weakCompareAndSet(this, canceledCallbacks ,null)) {
                //noinspection unchecked
                return (BiConsumer<InterruptedException, JContext>) canceledCallbacks;
            } else if (canceledCallbacks instanceof Queue) {
                //noinspection unchecked
                Queue<BiConsumer<InterruptedException, JContext>> callbacks = (Queue<BiConsumer<InterruptedException, JContext>>) canceledCallbacks;
                return callbacks.poll();
            }
        }
    }

    protected void offerFinallyCallbacks(TriConsumer<T, Throwable, JContext> callback) {
        while (true) {
            Object finallyCallbacks = this.finallyCallbacks;
            if (finallyCallbacks instanceof TriConsumer) {
                Queue<TriConsumer<T, Throwable, JContext>> callbacks = new UnPaddedLockFreeArrayQueue0<>(2);
                if (this.finallyCallbacks == finallyCallbacks && FINALLY_CALLBACKS.weakCompareAndSet(this, finallyCallbacks, callbacks)) {
                    //noinspection unchecked
                    callbacks.offer((TriConsumer<T, Throwable, JContext>) finallyCallbacks);
                    callbacks.offer(callback);
                    return;
                }
            } else if (finallyCallbacks instanceof Queue) {
                //noinspection unchecked
                Queue<TriConsumer<T, Throwable, JContext>> callbacks = (Queue<TriConsumer<T, Throwable, JContext>>) finallyCallbacks;
                callbacks.offer(callback);
                return;
            } else if (finallyCallbacks == null && FINALLY_CALLBACKS.weakCompareAndSet(this, null, callback)) {
                return;
            }
        }
    }

    protected TriConsumer<T, Throwable, JContext> pollFinallyCallback() {
        while (true) {
            Object finallyCallbacks = this.finallyCallbacks;
            if (finallyCallbacks == null) {
                return null;
            } else if (finallyCallbacks instanceof TriConsumer && FINALLY_CALLBACKS.weakCompareAndSet(this, finallyCallbacks ,null)) {
                //noinspection unchecked
                return (TriConsumer<T, Throwable, JContext>) finallyCallbacks;
            } else if (finallyCallbacks instanceof Queue) {
                //noinspection unchecked
                Queue<TriConsumer<T, Throwable, JContext>> callbacks = (Queue<TriConsumer<T, Throwable, JContext>>) finallyCallbacks;
                return callbacks.poll();
            }
        }
    }

    protected void offerRequestCancelCallback(Runnable callback) {
        while (true) {
            Object requestCancelCallback = this.requestCancelCallback;
            if (requestCancelCallback instanceof Runnable) {
                Queue<Runnable> callbacks = new UnPaddedLockFreeArrayQueue0<>(2);
                if (this.requestCancelCallback == requestCancelCallback && REQUEST_CANCEL_CALLBACK.weakCompareAndSet(this, requestCancelCallback, callbacks)) {
                    callbacks.offer((Runnable) requestCancelCallback);
                    callbacks.offer(callback);
                    return;
                }
            } else if (requestCancelCallback instanceof Queue) {
                //noinspection unchecked
                Queue<Runnable> callbacks = (Queue<Runnable>) requestCancelCallback;
                callbacks.offer(callback);
                return;
            } else if (requestCancelCallback == null && REQUEST_CANCEL_CALLBACK.weakCompareAndSet(this, null, callback)) {
                return;
            }
        }
    }

    protected Runnable pollRequestCancelCallback() {
        while (true) {
            Object requestCancelCallback = this.requestCancelCallback;
            if (requestCancelCallback == null) {
                return null;
            } else if (requestCancelCallback instanceof Runnable && REQUEST_CANCEL_CALLBACK.weakCompareAndSet(this, requestCancelCallback ,null)) {
                return (Runnable) requestCancelCallback;
            } else if (requestCancelCallback instanceof Queue) {
                //noinspection unchecked
                Queue<Runnable> callbacks = (Queue<Runnable>) requestCancelCallback;
                return callbacks.poll();
            }
        }
    }

    @Override
    public Throwable getError() {
        return error;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public boolean isCanceled() {
        return state == ST_REJECTED && error instanceof InterruptedException;
    }

    private <R> void queueNextPromise(JPromise<R> nextPromise) {
        while (state < ST_UNCOMPLETED) {
            if (state == ST_INIT && STATE.weakCompareAndSet(this, ST_INIT, ST_INIT_READ)) {
                try {
                    offerChild(nextPromise);
                    return;
                } finally {
                    state = ST_INIT;
                }
            } else if (state == ST_INIT_TERMING && STATE.weakCompareAndSet(this, ST_INIT_TERMING, ST_INIT_READ)) {
                try {
                    offerChild(nextPromise);
                    return;
                } finally {
                    state = ST_INIT_TERMING;
                }
            } else if (state == ST_RUNNING && STATE.weakCompareAndSet(this, ST_RUNNING, ST_RUNNING_READ)) {
                try {
                    offerChild(nextPromise);
                    return;
                } finally {
                    state = ST_RUNNING;
                }
            } else if (state == ST_RUNNING_TERMING && STATE.weakCompareAndSet(this, ST_RUNNING_TERMING, ST_RUNNING_READ)) {
                try {
                    offerChild(nextPromise);
                    return;
                } finally {
                    state = ST_RUNNING_TERMING;
                }
            } else if (state == ST_RESOLVING && STATE.weakCompareAndSet(this, ST_RESOLVING, ST_RESOLVING_BUSY)) {
                try {
                    offerChild(nextPromise);
                    return;
                } finally {
                    state = ST_RESOLVING;
                }
            } else if (state == ST_REJECTING && STATE.weakCompareAndSet(this, ST_REJECTING, ST_REJECTING_BUSY)) {
                try {
                    offerChild(nextPromise);
                    return;
                } finally {
                    state = ST_REJECTING;
                }
            }
        }
    }

    private <R> void thenCreator(JThunk<R> thunk, JContext context, JAsyncPromiseFunction1<T, R> mapper) {
        if (isResolved()) {
            try {
                JPromise<R> next = mapper.apply(value, context);
                next = next != null ? next : JPromise.empty();
                next.onError(thunk::reject)
                        .onSuccess(thunk::resolve)
                        .onCanceled((BiConsumer<InterruptedException, JContext>) thunk::interrupt)
                        .async(context);
                thunk.onRequestCancel(next::cancel);
            } catch (Throwable throwable) {
                thunk.reject(throwable, context);
            }
        } else if (isRejected()){
            thunk.reject(error, context);
        }
    }

    public <R> JPromise<R> doThen(JAsyncPromiseFunction1<T, R> mapper, boolean immediate, long delay, TimeUnit timeUnit) {
        if (mapper == null) {
            throw new NullPointerException();
        }
        JPromise<R> nextPromise = immediate
                ? generate((thunk, context) -> thenCreator(thunk, context, mapper), this)
                : create((jThunk, context) -> thenCreator(jThunk, context, mapper), delay, timeUnit, this);
        queueNextPromise(nextPromise);
        assert state != ST_UNCOMPLETED;
        return nextPromise;
    }

    @Override
    public <R> JPromise<R> thenWithContext(JAsyncPromiseFunction1<T, R> mapper, boolean immediate) {
        return doThen(mapper, immediate, 0, TimeUnit.MILLISECONDS);
    }

    @Override
    public JPromise<T> delay(long timeout, TimeUnit unit) {
        return doThen((v, ctx) -> JPromise.just(v), false, timeout, unit);
    }

    private void catchCreator(JThunk<T> thunk, JContext context, JAsyncCatchFunction1<Throwable, T> catcher) {
        if (isResolved()) {
            thunk.resolve(value, context);
        } else if (isRejected()) {
            try {
                JPromise<T> next = catcher.apply(error, context);
                next = next != null ? next : JPromise.empty();
                next.onError(thunk::reject)
                        .onSuccess(thunk::resolve)
                        .onCanceled((BiConsumer<InterruptedException, JContext>) thunk::interrupt)
                        .async(context);
                thunk.onRequestCancel(next::cancel);
            } catch (Throwable t) {
                thunk.reject(t, context);
            }
        }
    }

    @Override
    public JPromise<T> doCatchWithContext(JAsyncCatchFunction1<Throwable, T> catcher, boolean immediate) {
        if (catcher == null) {
            throw new NullPointerException();
        }
        JPromise<T> nextPromise = immediate
                ? generate((jThunk, context) -> catchCreator(jThunk, context, catcher), this)
                : create((jThunk, context) -> catchCreator(jThunk, context, catcher), this);
        queueNextPromise(nextPromise);
        assert state != ST_UNCOMPLETED;
        return nextPromise;
    }

    private <R> void thenOrCatchCreator(JThunk<R> thunk, JContext context, JAsyncPromiseFunction3<T, R> handler) {
        T v = isResolved() ? value : null;
        Throwable t = isRejected() ? error : null;
        try {
            JPromise<R> next = handler.apply(v, t, context);
            next = next != null ? next : JPromise.empty();
            next.onError(thunk::reject)
                    .onSuccess(thunk::resolve)
                    .onCanceled((BiConsumer<InterruptedException, JContext>) thunk::interrupt)
                    .async(context);
            thunk.onRequestCancel(next::cancel);
        } catch (Throwable throwable) {
            thunk.reject(throwable, context);
        }
    }

    @Override
    public <R> JPromise<R> thenOrCatchWithContext(JAsyncPromiseFunction3<T, R> handler, boolean immediate) {
        if (handler == null) {
            throw new NullPointerException();
        }
        JPromise<R> nextPromise = immediate
                ? generate((thunk, context) -> thenOrCatchCreator(thunk, context, handler), this)
                : create((jThunk, context) -> thenOrCatchCreator(jThunk, context, handler), this);
        queueNextPromise(nextPromise);
        assert state != ST_UNCOMPLETED;
        return nextPromise;
    }

    private <R> void finallyCreator(JThunk<T> thunk, JContext context, JAsyncPromiseSupplier1<R> supplier) {
        try {
            JPromise<R> next = supplier.get(context);
            next = next != null ? next : JPromise.empty();
            if (isResolved()) {
                next.onSuccess((ignored, ctx) -> thunk.resolve(value, ctx))
                        .onError(thunk::reject)
                        .onCanceled((BiConsumer<InterruptedException, JContext>) thunk::interrupt)
                        .async(context);
            } else {
                next.onSuccess((ignored, ctx) -> thunk.reject(error, ctx))
                        .onError(thunk::reject)
                        .onCanceled((BiConsumer<InterruptedException, JContext>) thunk::interrupt)
                        .async(context);
            }
            thunk.onRequestCancel(next::cancel);
        } catch (Throwable t) {
            thunk.reject(t, context);
        }
    }

    @Override
    public <R> JPromise<T> doFinallyWithContext(JAsyncPromiseSupplier1<R> supplier, boolean immediate) {
        if (supplier == null) {
            throw new NullPointerException();
        }
        JPromise<T> nextPromise = immediate
                ? generate((jThunk, context) -> finallyCreator(jThunk, context, supplier), this)
                : create((jThunk, context) -> finallyCreator(jThunk, context, supplier), this);
        queueNextPromise(nextPromise);
        assert state != ST_UNCOMPLETED;
        return nextPromise;
    }

    private void checkUncompleted(String errorMessage) {
        if (state > ST_COMPLETING) {
            throw new IllegalStateException(errorMessage);
        }
    }

    @Override
    public JPromise<T> onSuccess(BiConsumer<T, JContext> callback) {
        checkUncompleted("onSuccess must called before promise completed.");
        offerSuccessCallbacks(callback);
        checkUncompleted("onSuccess must called before promise completed.");
        return this;
    }

    @Override
    public JPromise<T> onError(BiConsumer<Throwable, JContext> callback) {
        checkUncompleted("onError must called before promise completed.");
        offerErrorCallbacks(callback);
        checkUncompleted("onError must called before promise completed.");
        return this;
    }

    @Override
    public JPromise<T> onFinally(TriConsumer<T, Throwable, JContext> callback) {
        checkUncompleted("onFinally must called before promise completed.");
        offerFinallyCallbacks(callback);
        checkUncompleted("onFinally must called before promise completed.");
        return this;
    }

    @Override
    public JPromise<T> onCanceled(BiConsumer<InterruptedException, JContext> callback) {
        checkUncompleted("onCanceled must called before promise completed.");
        offerCanceledCallbacks(callback);
        checkUncompleted("onCanceled must called before promise completed.");
        return this;
    }

    @Override
    public void onRequestCancel(Runnable runnable) {
        offerRequestCancelCallback(runnable);
    }

    /**
     * try to schedule if has not scheduled.
     * @param context promise context
     * @return false if success, true if not.
     */
    protected boolean trySchedule(JContext context) {
        while (true) {
            if (state == ST_INIT && STATE.weakCompareAndSet(this, ST_INIT, ST_RUNNING)) {
                run(context);
                return false;
            } else if (state == ST_INIT_TERMING && STATE.weakCompareAndSet(this, ST_INIT_TERMING, ST_RUNNING_TERMING)) {
                reject(new InterruptedException(), context);
                return false;
            } else if (state == ST_RUNNING || state == ST_RUNNING_TERMING) {
                return false;
            } else if (state > ST_COMPLETING) {
                return true;
            }
        }
    }

    @Override
    public void schedule(JContext context) {
        if (trySchedule(context)) {
            throw new IllegalStateException("The promise has already been scheduled.");
        }
    }

    @Override
    public boolean isResolved() {
        return state == ST_RESOLVED;
    }

    @Override
    public boolean isRejected() {
        return state == ST_REJECTED;
    }

    @Override
    public void resolve(T result, JContext context) {
        if (state < ST_RUNNING) {
            throw new IllegalStateException("Call async first.");
        }
        boolean processed = false;
        while (state < ST_UNCOMPLETED) {
            if (state == ST_RUNNING && STATE.weakCompareAndSet(this, ST_RUNNING, ST_RUNNING_WRITE)) {
                try {
                    this.value = result;
                    processed = true;
                    break;
                } finally {
                    state = ST_RESOLVING;
                }
            } else if (state == ST_RUNNING_TERMING) {
                reject(new InterruptedException(), context);
            } else if (state == ST_RESOLVING || state == ST_REJECTING) {
                break;
            }
        }
        if (processed) {
            BiConsumer<T, JContext> successCallback;
            JAsyncAfterResolvedException exception = null;
            while ((successCallback = pollSuccessCallback()) != null) {
                try {
                    successCallback.accept(value, context);
                } catch (Throwable t) {
                    if (exception == null) {
                        exception = new JAsyncAfterResolvedException(value);
                    }
                    exception.addSuppressed(t);
                }
            }
            TriConsumer<T, Throwable, JContext> finallyCallback;
            while ((finallyCallback = pollFinallyCallback()) != null) {
                try {
                    finallyCallback.accept(value, null, context);
                } catch (Throwable t) {
                    if (exception == null) {
                        exception = new JAsyncAfterResolvedException(value);
                    }
                    exception.addSuppressed(t);
                }
            }
            if (exception != null) {
                value = null;
                error = exception;
                while (true) {
                    if (state == ST_RESOLVING && STATE.weakCompareAndSet(this, ST_RESOLVING, ST_REJECTED)) {
                        break;
                    }
                }
                exception.printStackTrace();
            } else {
                while (true) {
                    if (state == ST_RESOLVING && STATE.weakCompareAndSet(this, ST_RESOLVING, ST_RESOLVED)) {
                        break;
                    }
                }
            }
            JPromise<?> promise;
            while ((promise = pollChildren()) != null) {
                promise.schedule(context);
            }
        }
    }

    @Override
    public void reject(Throwable error, JContext context) {
        if (state < ST_RUNNING) {
            throw new IllegalStateException("Call async first.");
        }
        context.fixException(error);
        boolean processed = false;
        while (state < ST_UNCOMPLETED) {
            if (state == ST_RUNNING && STATE.weakCompareAndSet(this, ST_RUNNING, ST_RUNNING_WRITE)) {
                try {
                    this.error = error;
                    processed = true;
                    break;
                } finally {
                    state = ST_REJECTING;
                }
            } else if (state == ST_RUNNING_TERMING && STATE.weakCompareAndSet(this, ST_RUNNING_TERMING, ST_RUNNING_WRITE)) {
                try {
                    if (!(error instanceof InterruptedException)) {
                        InterruptedException newError = new InterruptedException();
                        newError.addSuppressed(error);
                        error = newError;
                    }
                    this.error = error;
                    processed = true;
                    break;
                } finally {
                    state = ST_REJECTING;
                }
            } else if (state == ST_RESOLVING || state == ST_REJECTING) {
                break;
            }
        }
        if (processed) {
            JAsyncException exception = null;
            if (this.error instanceof InterruptedException) {
                BiConsumer<InterruptedException, JContext> cancelCallback;
                while ((cancelCallback = pollCancelCallback()) != null) {
                    try {
                        cancelCallback.accept((InterruptedException) this.error, context);
                    } catch (Throwable t) {
                        if (exception == null) {
                            exception = new JAsyncAfterCanceledException();
                        }
                        exception.addSuppressed(t);
                    }
                }
            } else {
                BiConsumer<Throwable, JContext> errorCallback;
                while ((errorCallback = pollErrorCallback()) != null) {
                    try {
                        errorCallback.accept(this.error, context);
                    } catch (Throwable t) {
                        if (exception == null) {
                            exception = new JAsyncAfterRejectedException(this.error);
                        }
                        exception.addSuppressed(t);
                    }
                }
            }
            TriConsumer<T, Throwable, JContext> finallyCallback;
            while ((finallyCallback = pollFinallyCallback()) != null) {
                try {
                    finallyCallback.accept(null, this.error, context);
                } catch (Throwable t) {
                    if (exception == null) {
                        exception = new JAsyncAfterRejectedException(this.error);
                    }
                    exception.addSuppressed(t);
                }
            }
            if (exception != null) {
                this.error = exception;
            }
            while (true) {
                if (state == ST_REJECTING && STATE.weakCompareAndSet(this, ST_REJECTING, ST_REJECTED)) {
                    break;
                }
            }
            JPromise<?> promise;
            while ((promise = pollChildren()) != null) {
                promise.schedule(context);
            }
        }
    }

    protected abstract void dispose();

    protected boolean tryCancel() {
        if (parent == null || parent.tryCancel()) {
            boolean running = false;
            boolean processed = false;
            while (true) {
                if (state == ST_INIT && STATE.weakCompareAndSet(this, ST_INIT, ST_INIT_TERMING)) {
                    processed = parent == null;
                    break;
                } else if (state == ST_RUNNING && STATE.weakCompareAndSet(this, ST_RUNNING, ST_RUNNING_TERMING)) {
                    running = true;
                    processed = true;
                    break;
                } else if (state == ST_INIT_TERMING || state == ST_RUNNING_TERMING) {
                    processed = true;
                    break;
                } else if (state > ST_COMPLETING) {
                    break;
                }
            }
            if (running) {
                dispose();
                Runnable callback;
                while ((callback = pollRequestCancelCallback()) != null) {
                    try {
                        callback.run();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
            return !processed;
        } else {
            return false;
        }
    }

    @Override
    public void cancel() {
        tryCancel();
    }

    @Override
    public T block(JContext context) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        while (state < ST_COMPLETING) {
            if (state == ST_INIT && STATE.weakCompareAndSet(this, ST_INIT, ST_INIT_READ)) {
                try {
                    onFinally((v, e, ctx) -> {
                        latch.countDown();
                    });
                    break;
                } finally {
                    state = ST_INIT;
                    async(context);
                    latch.await();
                }
            } else if (state == ST_INIT_TERMING && STATE.weakCompareAndSet(this, ST_INIT_TERMING, ST_INIT_READ)) {
                try {
                    onFinally((v, e, ctx) -> {
                        latch.countDown();
                    });
                    break;
                } finally {
                    state = ST_INIT_TERMING;
                    async(context);
                    latch.await();
                }
            } else if (state == ST_RUNNING && STATE.weakCompareAndSet(this, ST_RUNNING, ST_RUNNING_READ)) {
                try {
                    onFinally((v, e, ctx) -> {
                        latch.countDown();
                    });
                    break;
                } finally {
                    state = ST_RUNNING;
                    latch.await();
                }
            } else if (state == ST_RUNNING_TERMING && STATE.weakCompareAndSet(this, ST_RUNNING_TERMING, ST_RUNNING_READ)) {
                try {
                    onFinally((v, e, ctx) -> {
                        latch.countDown();
                    });
                    break;
                } finally {
                    state = ST_RUNNING_TERMING;
                    latch.await();
                }
            }
        }
        while (state < ST_UNCOMPLETED) {
            //noinspection BusyWait
            Thread.sleep(0);
        }
        if (error != null) {
            if (error instanceof InterruptedException) {
                throw (InterruptedException) error;
            } else if (error instanceof JAsyncWrapException) {
                JAsyncWrapException e = (JAsyncWrapException) error;
                if (e.getCause() instanceof InterruptedException) {
                    throw (InterruptedException) e.getCause();
                } else {
                    throw e;
                }
            } else {
                throw new JAsyncWrapException(error);
            }
        }
        return value;
    }

    protected boolean tryAsync(JContext context) {
        if (parent == null || parent.tryAsync(context)) {
            return trySchedule(context);
        } else {
            return false;
        }
    }

    @Override
    public JHandle<T> async(JContext context) {
        tryAsync(context);
        return this;
    }
}
