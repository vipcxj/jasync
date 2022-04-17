package io.github.vipcxj.jasync.ng.runtime.promise;

import io.github.vipcxj.jasync.ng.runtime.schedule.ImmediateTask;
import io.github.vipcxj.jasync.ng.runtime.schedule.LazyTask;
import io.github.vipcxj.jasync.ng.runtime.schedule.Task;
import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JHandle;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JThunk;
import io.github.vipcxj.jasync.ng.spec.functional.JAsyncCatchFunction1;
import io.github.vipcxj.jasync.ng.spec.functional.JAsyncPromiseFunction1;
import io.github.vipcxj.jasync.ng.spec.functional.JAsyncPromiseSupplier1;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class BasePromise<T> extends AbstractPromise<T> {
    private T value;
    private Throwable error;
    protected final Task<T> task;
    private JContext context;
    private final JPromise<?> parent;
    private List<BiConsumer<T, JContext>> successHandlers;
    private List<BiConsumer<Throwable, JContext>> errorHandlers;
    private List<Consumer<JContext>> completeHandlers;

    public BasePromise(Task<T> task) {
        this(task, null);
    }

    public BasePromise(Task<T> task, JPromise<?> parent) {
        this.parent = parent;
        this.task = task;
    }

    @Override
    public Task<T> getTask() {
        return task;
    }

    public static <T> BasePromise<T> generate(BiConsumer<JThunk<T>, JContext> handler, JPromise<?> parent) {
        return new BasePromise<>(new ImmediateTask<>(handler), parent);
    }

    public static <T> BasePromise<T> create(BiConsumer<JThunk<T>, JContext> handler, long delay, TimeUnit timeUnit, JPromise<?> parent) {
        return new BasePromise<>(new LazyTask<>(handler, delay, timeUnit), parent);
    }

    public static <T> BasePromise<T> create(BiConsumer<JThunk<T>, JContext> handler, JPromise<?> parent) {
        return new BasePromise<>(new LazyTask<>(handler), parent);
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

    private <R> void thenCreator(JThunk<R> thunk, JContext context, JAsyncPromiseFunction1<T, R> mapper) {
        if (isResolved()) {
            try {
                JPromise<R> next = mapper.apply(value, context);
                next = next != null ? next : JPromise.empty();
                next.onError(thunk::reject)
                        .onSuccess(thunk::resolve).async(context);
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
        BasePromise<R> nextPromise = immediate
                ? generate((jThunk, context) -> thenCreator(jThunk, context, mapper), this)
                : create((jThunk, context) -> thenCreator(jThunk, context, mapper), delay, timeUnit, this);
        getChildren().add(nextPromise);
        if (isCompleted()) {
            nextPromise.schedule(context);
        }
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
                next.onError(thunk::reject).onSuccess(thunk::resolve).async(context);
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
        BasePromise<T> nextPromise = immediate
                ? generate((jThunk, context) -> catchCreator(jThunk, context, catcher), this)
                : create((jThunk, context) -> catchCreator(jThunk, context, catcher), this);
        getChildren().add(nextPromise);
        if (isCompleted()) {
            nextPromise.schedule(context);
        }
        return nextPromise;
    }

    private <R> void finallyCreator(JThunk<T> thunk, JContext context, JAsyncPromiseSupplier1<R> supplier) {
        try {
            JPromise<R> next = supplier.get(context);
            next = next != null ? next : JPromise.empty();
            if (isResolved()) {
                next.onSuccess((ignored, ctx) -> resolve(value, ctx)).onError(thunk::reject).async(context);
            } else {
                next.onSuccess((ignored, ctx) -> reject(error, ctx)).onError(thunk::reject).async(context);
            }
        } catch (Throwable t) {
            thunk.reject(t, context);
        }
    }

    @Override
    public <R> JPromise<T> doFinallyWithContext(JAsyncPromiseSupplier1<R> supplier, boolean immediate) {
        if (supplier == null) {
            throw new NullPointerException();
        }
        BasePromise<T> nextPromise = immediate
                ? generate((thunk, context) -> finallyCreator(thunk, context, supplier), this)
                : create((thunk, context) -> finallyCreator(thunk, context, supplier), this);
        getChildren().add(nextPromise);
        if (isCompleted()) {
            nextPromise.schedule(context);
        }
        return nextPromise;
    }


    @Override
    public synchronized JPromise<T> onSuccess(BiConsumer<T, JContext> resolver) {
        if (resolved) {
            resolver.accept(value, context);
        } else {
            getSuccessHandlers().add(resolver);
        }
        return this;
    }

    @Override
    public synchronized JPromise<T> onError(BiConsumer<Throwable, JContext> reject) {
        if (rejected) {
            reject.accept(error, context);
        } else {
            getErrorHandlers().add(reject);
        }
        return this;
    }

    @Override
    public synchronized JPromise<T> onFinally(Consumer<JContext> consumer) {
        if (resolved || rejected) {
            consumer.accept(context);
        } else {
            getCompleteHandlers().add(consumer);
        }
        return this;
    }

    @Override
    public JPromise<T> onDispose(Runnable runnable) {
        if (disposed) {
            runnable.run();
        } else {
            getTerminalHandlers().add(runnable);
        }
        return this;
    }

    @Override
    public synchronized T block(JContext context) throws InterruptedException {
        async(context);
        while (!isCompleted() && !isDisposed()) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            }
        }
        if (isResolved()) {
            return value;
        } else if (isRejected()) {
            throw new RuntimeException(error);
        } else {
            throw new InterruptedException();
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

    @Override
    public void resolve(T result, JContext context) {
        if (isDisposed()) {
            return;
        }
        this.value = result;
        this.context = context;
        markResolved();
        try {
            try {
                triggerSuccessHandlers(context);
            } finally {
                triggerCompleteHandlers(context);
            }
        } finally {
            scheduleNext(context);
        }
    }

    @Override
    public void reject(Throwable error, JContext context) {
        if (isDisposed()) {
            return;
        }
        this.error = error;
        this.context = context;
        markRejected();
        try {
            try {
                triggerErrorHandlers(context);
            } finally {
                triggerCompleteHandlers(context);
            }
        } finally {
            scheduleNext(context);
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
