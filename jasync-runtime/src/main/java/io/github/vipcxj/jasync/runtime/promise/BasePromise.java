package io.github.vipcxj.jasync.runtime.promise;

import io.github.vipcxj.jasync.runtime.schedule.ImmediateTask;
import io.github.vipcxj.jasync.runtime.schedule.LazyTask;
import io.github.vipcxj.jasync.runtime.schedule.Task;
import io.github.vipcxj.jasync.spec.JContext;
import io.github.vipcxj.jasync.spec.JHandle;
import io.github.vipcxj.jasync.spec.JPromise2;
import io.github.vipcxj.jasync.spec.JThunk;
import io.github.vipcxj.jasync.spec.functional.JAsyncCatchFunction1;
import io.github.vipcxj.jasync.spec.functional.JAsyncPromiseFunction1;
import io.github.vipcxj.jasync.spec.functional.JAsyncPromiseSupplier1;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class BasePromise<T> implements JPromise2<T>, JThunk<T> {
    private T value;
    private Throwable error;
    private boolean started;
    private boolean resolved;
    private boolean rejected;
    private boolean disposed;
    protected final Task<T> task;
    private final JPromise2<?> parent;
    private List<BasePromise<?>> children;
    private List<BiConsumer<T, JContext>> successHandlers;
    private List<BiConsumer<Throwable, JContext>> errorHandlers;
    private List<Consumer<JContext>> completeHandlers;
    private List<Runnable> terminalHandlers;

    public BasePromise(Task<T> task) {
        this(task, null);
    }

    public BasePromise(Task<T> task, JPromise2<?> parent) {
        this.parent = parent;
        this.started = false;
        this.resolved = false;
        this.rejected = false;
        this.disposed = false;
        this.task = task;
    }

    public static <T> BasePromise<T> generate(BiConsumer<JThunk<T>, JContext> handler, JPromise2<?> parent) {
        return new BasePromise<>(new ImmediateTask<>(handler), parent);
    }

    public static <T> BasePromise<T> create(BiConsumer<JThunk<T>, JContext> handler, JPromise2<?> parent) {
        return new BasePromise<>(new LazyTask<>(handler), parent);
    }

    private List<BasePromise<?>> getChildren() {
        if (children == null) {
            children = new ArrayList<>();
        }
        return children;
    }

    private List<BiConsumer<T, JContext>> getSuccessHandlers() {
        if (successHandlers == null) {
            successHandlers = new ArrayList<>();
        }
        return successHandlers;
    }

    private List<BiConsumer<Throwable, JContext>> getErrorHandlers() {
        if (errorHandlers == null) {
            errorHandlers = new ArrayList<>();
        }
        return errorHandlers;
    }

    public List<Consumer<JContext>> getCompleteHandlers() {
        if (completeHandlers == null) {
            completeHandlers = new ArrayList<>();
        }
        return completeHandlers;
    }

    public List<Runnable> getTerminalHandlers() {
        if (terminalHandlers == null) {
            terminalHandlers = new ArrayList<>();
        }
        return terminalHandlers;
    }

    private <R> void thenCreator(JThunk<R> thunk, JContext context, JAsyncPromiseFunction1<T, R> mapper) {
        if (resolved) {
            try {
                JPromise2<R> next = mapper.apply(value, context);
                next = next != null ? next : JPromise2.empty();
                next.onError(thunk::reject)
                        .onSuccess(thunk::resolve).async(context);
            } catch (Throwable throwable) {
                thunk.reject(throwable, context);
            }
        } else if (rejected){
            thunk.reject(error, context);
        }
    }

    @Override
    public <R> JPromise2<R> thenWithContext(JAsyncPromiseFunction1<T, R> mapper, boolean immediate) {
        if (mapper == null) {
            throw new NullPointerException();
        }
        BasePromise<R> nextPromise = immediate
                ? generate((jThunk, context) -> thenCreator(jThunk, context, mapper), this)
                : create((jThunk, context) -> thenCreator(jThunk, context, mapper), this);
        getChildren().add(nextPromise);
        return nextPromise;
    }

    private void catchCreator(JThunk<T> thunk, JContext context, JAsyncCatchFunction1<Throwable, T> catcher) {
        if (resolved) {
            thunk.resolve(value, context);
        } else if (rejected) {
            try {
                JPromise2<T> next = catcher.apply(error, context);
                next = next != null ? next : JPromise2.empty();
                next.onError(thunk::reject).onSuccess(thunk::resolve).async(context);
            } catch (Throwable t) {
                thunk.reject(t, context);
            }
        }
    }

    @Override
    public JPromise2<T> doCatchWithContext(JAsyncCatchFunction1<Throwable, T> catcher, boolean immediate) {
        if (catcher == null) {
            throw new NullPointerException();
        }
        BasePromise<T> nextPromise = immediate
                ? generate((jThunk, context) -> catchCreator(jThunk, context, catcher), this)
                : create((jThunk, context) -> catchCreator(jThunk, context, catcher), this);
        getChildren().add(nextPromise);
        return nextPromise;
    }

    private <R> void finallyCreator(JThunk<T> thunk, JContext context, JAsyncPromiseSupplier1<R> supplier) {
        try {
            JPromise2<R> next = supplier.get(context);
            next = next != null ? next : JPromise2.empty();
            if (resolved) {
                next.onSuccess((ignored, ctx) -> resolve(value, ctx)).onError(thunk::reject).async(context);
            } else {
                next.onSuccess((ignored, ctx) -> reject(error, ctx)).onError(thunk::reject).async(context);
            }
        } catch (Throwable t) {
            thunk.reject(t, context);
        }
    }

    @Override
    public <R> JPromise2<T> doFinallyWithContext(JAsyncPromiseSupplier1<R> supplier, boolean immediate) {
        if (supplier == null) {
            throw new NullPointerException();
        }
        BasePromise<T> nextPromise = immediate
                ? generate((thunk, context) -> finallyCreator(thunk, context, supplier), this)
                : create((thunk, context) -> finallyCreator(thunk, context, supplier), this);
        getChildren().add(nextPromise);
        return nextPromise;
    }


    @Override
    public JPromise2<T> onSuccess(BiConsumer<T, JContext> resolver) {
        getSuccessHandlers().add(resolver);
        return this;
    }

    @Override
    public JPromise2<T> onError(BiConsumer<Throwable, JContext> reject) {
        getErrorHandlers().add(reject);
        return this;
    }

    @Override
    public JPromise2<T> onFinally(Consumer<JContext> consumer) {
        getCompleteHandlers().add(consumer);
        return this;
    }

    @Override
    public JPromise2<T> onDispose(Runnable runnable) {
        getTerminalHandlers().add(runnable);
        return this;
    }

    @Override
    public void schedule(JContext context) {
        if (!started && !disposed) {
            this.started = true;
            task.schedule(this, context);
        }
    }

    @Override
    public synchronized T block(JContext context) {
        async(context);
        while (!isCompleted()) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        if (isResolved()) {
            return value;
        } else {
            throw new RuntimeException(error);
        }
    }

    @Override
    public JHandle<T> async(JContext context) {
        if (parent != null) {
            parent.async(context);
        } else {
            schedule(context);
        }
        return this;
    }

    private void scheduleNext(JContext context) {
        if (children != null) {
            for (BasePromise<?> child : children) {
                child.schedule(context);
            }
        }
    }

    private void triggerSuccessHandlers(JContext context) {
        if (successHandlers != null) {
            for (BiConsumer<T, JContext> resolveHandler : successHandlers) {
                resolveHandler.accept(value, context);
            }
        }
    }

    private void triggerErrorHandlers(JContext context) {
        if (errorHandlers != null) {
            for (BiConsumer<Throwable, JContext> rejectHandler : errorHandlers) {
                rejectHandler.accept(error, context);
            }
        }
    }

    private void triggerCompleteHandlers(JContext context) {
        if (completeHandlers != null) {
            for (Consumer<JContext> completeHandler : completeHandlers) {
                completeHandler.accept(context);
            }
        }
    }

    private void triggerTerminalHandlers() {
        if (terminalHandlers != null) {
            for (Runnable handler : terminalHandlers) {
                handler.run();
            }
        }
    }

    private synchronized void markResolved() {
        this.resolved = true;
        this.rejected = false;
        notifyAll();
    }

    private synchronized void markRejected() {
        this.resolved = false;
        this.rejected = true;
        notifyAll();
    }

    protected void resolve(T result, JContext context, boolean next) {
        if (disposed) {
            return;
        }
        this.value = result;
        markResolved();
        try {
            try {
                triggerSuccessHandlers(context);
            } finally {
                triggerCompleteHandlers(context);
            }
        } finally {
            if (next) {
                scheduleNext(context);
            }
        }
    }

    @Override
    public void resolve(T result, JContext context) {
        resolve(result, context, true);
    }

    @Override
    public boolean isResolved() {
        return resolved;
    }

    protected void reject(Throwable error, JContext context, boolean next) {
        if (disposed) {
            return;
        }
        this.error = error;
        markRejected();
        try {
            try {
                triggerErrorHandlers(context);
            } finally {
                triggerCompleteHandlers(context);
            }
        } finally {
            if (next) {
                scheduleNext(context);
            }
        }
    }

    @Override
    public void reject(Throwable error, JContext context) {
        reject(error, context, true);
    }

    @Override
    public boolean isRejected() {
        return rejected;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        task.cancel();
        try {
            triggerTerminalHandlers();
        } finally {
            if (children != null) {
                for (BasePromise<?> child : children) {
                    child.dispose();
                }
            }
        }
    }

    @Override
    public void cancel() {
        if (parent != null) {
            parent.cancel();
        } else {
            dispose();
        }
    }
}
