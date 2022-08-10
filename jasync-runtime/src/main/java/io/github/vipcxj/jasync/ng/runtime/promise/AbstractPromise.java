package io.github.vipcxj.jasync.ng.runtime.promise;

import io.github.vipcxj.jasync.ng.runtime.schedule.ImmediateTask;
import io.github.vipcxj.jasync.ng.runtime.schedule.LazyTask;
import io.github.vipcxj.jasync.ng.runtime.utils.ImmutableDisposableStack;
import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JHandle;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JThunk;
import io.github.vipcxj.jasync.ng.spec.exceptions.JAsyncAfterRejectedException;
import io.github.vipcxj.jasync.ng.spec.exceptions.JAsyncAfterResolvedException;
import io.github.vipcxj.jasync.ng.spec.exceptions.JAsyncException;
import io.github.vipcxj.jasync.ng.spec.exceptions.JAsyncWrapException;
import io.github.vipcxj.jasync.ng.spec.functional.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BiConsumer;

public abstract class AbstractPromise<T> implements JPromise<T>, JThunk<T> {

    protected T value;
    protected Throwable error;
    protected volatile int state = ST_INIT;
    @SuppressWarnings("rawtypes")
    private static final AtomicIntegerFieldUpdater<AbstractPromise> STATE = AtomicIntegerFieldUpdater.newUpdater(AbstractPromise.class, "state");
    protected static final int ST_LOCK = 0;
    protected static final int ST_INIT = 1;
    protected static final int ST_INIT_TERMING = 2;
    protected static final int ST_RUNNING = 3;
    protected static final int ST_COMPLETING = 4;
    protected static final int ST_UNCOMPLETED = 5;
    protected static final int ST_RESOLVED = 6;
    protected static final int ST_REJECTED = 7;

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
    private volatile Object finallyCallbacks;
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<AbstractPromise, Object> FINALLY_CALLBACKS = AtomicReferenceFieldUpdater.newUpdater(AbstractPromise.class, Object.class, "finallyCallbacks");
    private volatile Object requestCancelCallback;
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<AbstractPromise, Object> REQUEST_CANCEL_CALLBACK = AtomicReferenceFieldUpdater.newUpdater(AbstractPromise.class, Object.class, "requestCancelCallback");
    private volatile Object suspendExceptions;
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<AbstractPromise, Object> SUSPEND_EXCEPTIONS = AtomicReferenceFieldUpdater.newUpdater(AbstractPromise.class, Object.class, "suspendExceptions");
    private volatile JContext context;


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
                ImmutableDisposableStack<? extends JPromise<?>> childrenContainer = ImmutableDisposableStack.create(child, (JPromise<?>) children);
                if (this.children == children && CHILDREN.weakCompareAndSet(this, children, childrenContainer)) {
                    return;
                }
            } else if (children instanceof ImmutableDisposableStack) {
                //noinspection unchecked
                ImmutableDisposableStack<JPromise<?>> childrenContainer = (ImmutableDisposableStack<JPromise<?>>) children;
                if (this.children == children && CHILDREN.weakCompareAndSet(this, children, childrenContainer.push(child))) {
                    return;
                }
            } else if (children == null) {
                if (CHILDREN.weakCompareAndSet(this, null, child)) {
                    return;
                }
            }
        }
    }

    protected void consumeChildren(JContext context) {
        Object children;
        do {
            children = this.children;
            if (children == null) {
                return;
            }
        } while (!CHILDREN.weakCompareAndSet(this, children, null));
        if (children instanceof JPromise) {
            ((JPromise<?>) children).schedule(context);
        } else if (children instanceof ImmutableDisposableStack) {
            //noinspection unchecked
            ImmutableDisposableStack<JPromise<?>> stack = (ImmutableDisposableStack<JPromise<?>>) children;
            do {
                JPromise<?> promise = stack.top();
                promise.schedule(context);
            } while ((stack = stack.pop()) != null);
        }
    }

    protected void offerSuccessCallbacks(BiConsumer<T, JContext> callback) {
        while (true) {
            Object successCallbacks = this.successCallbacks;
            if (successCallbacks instanceof BiConsumer) {
                //noinspection unchecked
                ImmutableDisposableStack<BiConsumer<T, JContext>> callbacks = ImmutableDisposableStack.create(callback, (BiConsumer<T, JContext>) successCallbacks);
                if (this.successCallbacks == successCallbacks && SUCCESS_CALLBACKS.weakCompareAndSet(this, successCallbacks, callbacks)) {
                    return;
                }
            } else if (successCallbacks instanceof ImmutableDisposableStack) {
                //noinspection unchecked
                ImmutableDisposableStack<BiConsumer<T, JContext>> callbacks = (ImmutableDisposableStack<BiConsumer<T, JContext>>) successCallbacks;
                if (this.successCallbacks == successCallbacks && SUCCESS_CALLBACKS.weakCompareAndSet(this, successCallbacks, callbacks.push(callback))) {
                    return;
                }
            } else if (successCallbacks == null) {
                if (SUCCESS_CALLBACKS.weakCompareAndSet(this, null, callback)) {
                    return;
                }
            }
        }
    }

    protected void consumeSuccessCallbacks(T value, JContext context) {
        Object successCallbacks;
        do {
            successCallbacks = this.successCallbacks;
            if (successCallbacks == null) {
                return;
            }
        } while (!SUCCESS_CALLBACKS.weakCompareAndSet(this, successCallbacks, null));
        JAsyncAfterResolvedException exception = null;
        if (successCallbacks instanceof BiConsumer) {
            try {
                //noinspection unchecked
                ((BiConsumer<T, JContext>) successCallbacks).accept(value, context);
            } catch (Throwable t) {
                exception = new JAsyncAfterResolvedException(value);
                exception.addSuppressed(t);
            }
        } else if (successCallbacks instanceof ImmutableDisposableStack) {
            //noinspection unchecked
            ImmutableDisposableStack<BiConsumer<T, JContext>> stack = (ImmutableDisposableStack<BiConsumer<T, JContext>>) successCallbacks;
            do {
                BiConsumer<T, JContext> callback = stack.top();
                try {
                    callback.accept(value, context);
                } catch (Throwable t) {
                    if (exception == null) {
                        exception = new JAsyncAfterResolvedException(value);
                    }
                    exception.addSuppressed(t);
                }
            } while ((stack = stack.pop()) != null);
        }
        if (exception != null) {
            throw exception;
        }
    }

    protected void offerErrorCallbacks(BiConsumer<Throwable, JContext> callback) {
        while (true) {
            Object errorCallbacks = this.errorCallbacks;
            if (errorCallbacks instanceof BiConsumer) {
                //noinspection unchecked
                ImmutableDisposableStack<BiConsumer<Throwable, JContext>> callbacks = ImmutableDisposableStack.create(callback, (BiConsumer<Throwable, JContext>) errorCallbacks);
                if (this.errorCallbacks == errorCallbacks && ERROR_CALLBACKS.weakCompareAndSet(this, errorCallbacks, callbacks)) {
                    return;
                }
            } else if (errorCallbacks instanceof ImmutableDisposableStack) {
                //noinspection unchecked
                ImmutableDisposableStack<BiConsumer<Throwable, JContext>> callbacks = (ImmutableDisposableStack<BiConsumer<Throwable, JContext>>) errorCallbacks;
                if (this.errorCallbacks == errorCallbacks && ERROR_CALLBACKS.weakCompareAndSet(this, errorCallbacks, callbacks.push(callback))) {
                    return;
                }
            } else if (errorCallbacks == null) {
                if (ERROR_CALLBACKS.weakCompareAndSet(this, null, callback)) {
                    return;
                }
            }
        }
    }

    protected void consumeErrorCallbacks(Throwable error, JContext context) {
        Object errorCallbacks;
        do {
            errorCallbacks = this.errorCallbacks;
            if (errorCallbacks == null) {
                return;
            }
        } while (!ERROR_CALLBACKS.weakCompareAndSet(this, errorCallbacks, null));
        JAsyncAfterRejectedException exception = null;
        if (errorCallbacks instanceof BiConsumer) {
            try {
                //noinspection unchecked
                ((BiConsumer<Throwable, JContext>) errorCallbacks).accept(error, context);
            } catch (Throwable t) {
                exception = new JAsyncAfterRejectedException(error);
                exception.addSuppressed(t);
            }
        } else if (errorCallbacks instanceof ImmutableDisposableStack) {
            //noinspection unchecked
            ImmutableDisposableStack<BiConsumer<Throwable, JContext>> stack = (ImmutableDisposableStack<BiConsumer<Throwable, JContext>>) errorCallbacks;
            do {
                BiConsumer<Throwable, JContext> callback = stack.top();
                try {
                    callback.accept(error, context);
                } catch (Throwable t) {
                    if (exception == null) {
                        exception = new JAsyncAfterRejectedException(error);
                    }
                    exception.addSuppressed(t);
                }
            } while ((stack = stack.pop()) != null);
        }
        if (exception != null) {
            throw exception;
        }
    }

    protected void offerFinallyCallbacks(TriConsumer<T, Throwable, JContext> callback) {
        while (true) {
            Object finallyCallbacks = this.finallyCallbacks;
            if (finallyCallbacks instanceof TriConsumer) {
                //noinspection unchecked
                ImmutableDisposableStack<TriConsumer<T, Throwable, JContext>> callbacks = ImmutableDisposableStack.create(callback, (TriConsumer<T, Throwable, JContext>) finallyCallbacks);
                if (this.finallyCallbacks == finallyCallbacks && FINALLY_CALLBACKS.weakCompareAndSet(this, finallyCallbacks, callbacks)) {
                    return;
                }
            } else if (finallyCallbacks instanceof ImmutableDisposableStack) {
                //noinspection unchecked
                ImmutableDisposableStack<TriConsumer<T, Throwable, JContext>> callbacks = (ImmutableDisposableStack<TriConsumer<T, Throwable, JContext>>) finallyCallbacks;
                if (this.finallyCallbacks == finallyCallbacks && FINALLY_CALLBACKS.weakCompareAndSet(this, finallyCallbacks, callbacks.push(callback))) {
                    return;
                }
            } else if (finallyCallbacks == null) {
                if (FINALLY_CALLBACKS.weakCompareAndSet(this, null, callback)) {
                    return;
                }
            }
        }
    }

    protected void consumeFinallyCallbacks(T value, Throwable error, JContext context, JAsyncException exception) {
        Object finallyCallbacks;
        do {
            finallyCallbacks = this.finallyCallbacks;
            if (finallyCallbacks == null) {
                return;
            }
        } while (!FINALLY_CALLBACKS.weakCompareAndSet(this, finallyCallbacks, null));
        if (finallyCallbacks instanceof TriConsumer) {
            try {
                //noinspection unchecked
                ((TriConsumer<T, Throwable, JContext>) finallyCallbacks).accept(value, error, context);
            } catch (Throwable t) {
                if (exception == null) {
                    exception = error == null ? new JAsyncAfterResolvedException(value) : new JAsyncAfterRejectedException(error);
                }
                exception.addSuppressed(t);
            }
        } else if (finallyCallbacks instanceof ImmutableDisposableStack) {
            //noinspection unchecked
            ImmutableDisposableStack<TriConsumer<T, Throwable, JContext>> stack = (ImmutableDisposableStack<TriConsumer<T, Throwable, JContext>>) finallyCallbacks;
            do {
                TriConsumer<T, Throwable, JContext> callback = stack.top();
                try {
                    callback.accept(value, error, context);
                } catch (Throwable t) {
                    if (exception == null) {
                        exception = error == null ? new JAsyncAfterResolvedException(value) : new JAsyncAfterRejectedException(error);
                    }
                    exception.addSuppressed(t);
                }
            } while ((stack = stack.pop()) != null);
        }
        if (exception != null) {
            throw exception;
        }
    }

    protected void offerRequestCancelCallback(Runnable callback) {
        while (true) {
            Object requestCancelCallback = this.requestCancelCallback;
            if (requestCancelCallback instanceof Runnable) {
                ImmutableDisposableStack<Runnable> callbacks = ImmutableDisposableStack.create(callback, (Runnable) requestCancelCallback);
                if (this.requestCancelCallback == requestCancelCallback && REQUEST_CANCEL_CALLBACK.weakCompareAndSet(this, requestCancelCallback, callbacks)) {
                    return;
                }
            } else if (requestCancelCallback instanceof ImmutableDisposableStack) {
                //noinspection unchecked
                ImmutableDisposableStack<Runnable> callbacks = (ImmutableDisposableStack<Runnable>) requestCancelCallback;
                if (this.requestCancelCallback == requestCancelCallback && REQUEST_CANCEL_CALLBACK.weakCompareAndSet(this, requestCancelCallback, callbacks.push(callback))) {
                    return;
                }
            } else if (requestCancelCallback == null) {
                if (REQUEST_CANCEL_CALLBACK.weakCompareAndSet(this, null, callback)) {
                    return;
                }
            }
        }
    }

    protected void consumeRequestCancelCallbacks() {
        Object requestCancelCallback;
        do {
            requestCancelCallback = this.requestCancelCallback;
            if (requestCancelCallback == null) {
                return;
            }
        } while (!REQUEST_CANCEL_CALLBACK.weakCompareAndSet(this, requestCancelCallback, null));
        if (requestCancelCallback instanceof Runnable) {
            try {
                ((Runnable) requestCancelCallback).run();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        } else if (requestCancelCallback instanceof ImmutableDisposableStack) {
            //noinspection unchecked
            ImmutableDisposableStack<Runnable> stack = (ImmutableDisposableStack<Runnable>) requestCancelCallback;
            do {
                Runnable callback = stack.top();
                try {
                    callback.run();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            } while ((stack = stack.pop()) != null);
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

    private <R> void thenCreator(JThunk<R> thunk, JContext context, JAsyncPromiseFunction1<T, R> mapper) {
        if (isResolved()) {
            try {
                JPromise<R> next = mapper.apply(value, context);
                next = next != null ? next : JPromise.empty();
                next.onError(thunk::reject)
                        .onSuccess(thunk::resolve)
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
        offerChild(nextPromise);
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
        offerChild(nextPromise);
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
        offerChild(nextPromise);
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
                        .async(context);
            } else {
                next.onSuccess((ignored, ctx) -> thunk.reject(error, ctx))
                        .onError(thunk::reject)
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
        offerChild(nextPromise);
        assert state != ST_UNCOMPLETED;
        return nextPromise;
    }

    @Override
    public JPromise<T> onSuccess(BiConsumer<T, JContext> callback) {
        offerSuccessCallbacks(callback);
        return this;
    }

    @Override
    public JPromise<T> onError(BiConsumer<Throwable, JContext> callback) {
        offerErrorCallbacks(callback);
        return this;
    }

    @Override
    public JPromise<T> onFinally(TriConsumer<T, Throwable, JContext> callback) {
        offerFinallyCallbacks(callback);
        return this;
    }

    @Override
    public void onRequestCancel(Runnable runnable) {
        offerRequestCancelCallback(runnable);
    }

    @Override
    public void schedule(JContext context) {
        while (true) {
            if (state == ST_INIT && STATE.weakCompareAndSet(this, ST_INIT, ST_RUNNING)) {
                this.context = context;
                run(context);
                break;
            } else if (state == ST_INIT_TERMING && STATE.weakCompareAndSet(this, ST_INIT_TERMING, ST_RUNNING)) {
                this.context = context;
                reject(new InterruptedException(), context);
                break;
            } else if (state == ST_RUNNING) {
                break;
            } else if (state == ST_RESOLVED) {
                afterResolved(context);
                break;
            } else if (state == ST_REJECTED) {
                afterRejected(context);
                break;
            }
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

    private void afterResolved(JContext context) {
        JAsyncAfterResolvedException exception = null;
        try {
            consumeSuccessCallbacks(value, context);
        } catch (JAsyncAfterResolvedException e) {
            exception = e;
        } finally {
            try {
                consumeFinallyCallbacks(value, null, context, exception);
            } catch (JAsyncAfterResolvedException e) {
                exception = e;
            }
        }
        if (exception != null) {
            offerSuspendException(exception);
        }
        consumeChildren(context);
    }

    @Override
    public void resolve(T result, JContext context) {
        if (state < ST_RUNNING) {
            throw new IllegalStateException("Call async first.");
        }
        boolean processed = false;
        while (state < ST_UNCOMPLETED) {
            if (state == ST_RUNNING && STATE.weakCompareAndSet(this, ST_RUNNING, ST_LOCK)) {
                try {
                    this.value = result;
                    this.context = null;
                    processed = true;
                    break;
                } finally {
                    state = ST_RESOLVED;
                }
            }
        }
        if (processed) {
            afterResolved(context);
        }
    }

    private void afterRejected(JContext context) {
        JAsyncAfterRejectedException exception = null;
        try {
            consumeErrorCallbacks(error, context);
        } catch (JAsyncAfterRejectedException e) {
            exception = e;
        } finally {
            try {
                consumeFinallyCallbacks(null, error, context, exception);
            } catch (JAsyncAfterRejectedException e) {
                exception = e;
            }
        }
        if (exception != null) {
            offerSuspendException(exception);
        }
        consumeChildren(context);
    }

    @Override
    public void reject(Throwable error, JContext context) {
        if (state < ST_RUNNING) {
            throw new IllegalStateException("Call async first.");
        }
        context.fixException(error);
        boolean processed = false;
        while (state < ST_UNCOMPLETED) {
            if (state == ST_RUNNING && STATE.weakCompareAndSet(this, ST_RUNNING, ST_LOCK)) {
                try {
                    this.error = error;
                    this.context = null;
                    processed = true;
                    break;
                } finally {
                    state = ST_REJECTED;
                }
            }
        }
        if (processed) {
            afterRejected(context);
        }
    }

    private void offerSuspendException(Throwable t) {
        if (parent == null) {
            while (true) {
                Object suspendExceptions = this.suspendExceptions;
                if (suspendExceptions instanceof Throwable) {
                    ImmutableDisposableStack<Throwable> throwables = ImmutableDisposableStack.create(t, (Throwable) suspendExceptions);
                    if (this.suspendExceptions == suspendExceptions && SUSPEND_EXCEPTIONS.weakCompareAndSet(this, suspendExceptions, throwables)) {
                        return;
                    }
                } else if (suspendExceptions instanceof ImmutableDisposableStack) {
                    //noinspection unchecked
                    ImmutableDisposableStack<Throwable> throwables = (ImmutableDisposableStack<Throwable>) suspendExceptions;
                    if (this.suspendExceptions == suspendExceptions && SUSPEND_EXCEPTIONS.weakCompareAndSet(this, suspendExceptions, throwables.push(t))) {
                        return;
                    }
                } else if (suspendExceptions == null) {
                    if (SUSPEND_EXCEPTIONS.weakCompareAndSet(this, null, t)) {
                        return;
                    }
                }
            }
        } else {
            parent.offerSuspendException(t);
        }
    }

    @Override
    public List<Throwable> getSuspendThrowables() {
        Object suspendExceptions;
        do {
            suspendExceptions = this.suspendExceptions;
            if (suspendExceptions == null) {
                return Collections.emptyList();
            }
        } while (!SUSPEND_EXCEPTIONS.weakCompareAndSet(this, suspendExceptions, null));
        List<Throwable> throwables = new ArrayList<>();
        if (suspendExceptions instanceof Throwable) {
            throwables.add((Throwable) suspendExceptions);
        } else {
            //noinspection unchecked
            ImmutableDisposableStack<Throwable> stack = (ImmutableDisposableStack<Throwable>) suspendExceptions;
            do {
                Throwable throwable = stack.top();
                throwables.add(throwable);
            } while ((stack = stack.pop()) != null);
        }
        return throwables;
    }

    protected abstract void dispose();

    protected boolean tryCancel() {
        if (parent == null || parent.tryCancel()) {
            boolean running = false;
            boolean processed = false;
            while (true) {
                int state = this.state;
                if (state == ST_INIT && STATE.weakCompareAndSet(this, ST_INIT, ST_INIT_TERMING)) {
                    processed = parent == null;
                    break;
                } else if (state == ST_RUNNING) {
                    while (true) {
                        JContext context = this.context;
                        if (context != null) {
                            InterruptedException exception = new InterruptedException();
                            reject(exception, context);
                            if (error == exception) {
                                running = true;
                                processed = true;
                                break;
                            }
                        }
                        if (this.state > ST_COMPLETING) {
                            break;
                        }
                    }
                    break;
                } else if (state == ST_INIT_TERMING) {
                    processed = true;
                    break;
                } else if (state > ST_COMPLETING) {
                    break;
                }
            }
            if (running) {
                dispose();
                consumeRequestCancelCallbacks();
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
            if (state == ST_INIT && STATE.weakCompareAndSet(this, ST_INIT, ST_LOCK)) {
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
            } else if (state == ST_INIT_TERMING && STATE.weakCompareAndSet(this, ST_INIT_TERMING, ST_LOCK)) {
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
            } else if (state == ST_RUNNING && STATE.weakCompareAndSet(this, ST_RUNNING, ST_LOCK)) {
                try {
                    onFinally((v, e, ctx) -> {
                        latch.countDown();
                    });
                    break;
                } finally {
                    state = ST_RUNNING;
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

    @Override
    public JHandle<T> async(JContext context) {
        if (parent == null) {
            schedule(context);
        } else {
            parent.async(context);
        }
        return this;
    }
}
