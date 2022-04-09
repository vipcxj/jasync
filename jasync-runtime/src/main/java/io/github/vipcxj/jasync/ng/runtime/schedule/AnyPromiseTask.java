package io.github.vipcxj.jasync.ng.runtime.schedule;

import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JThunk;
import io.github.vipcxj.jasync.ng.spec.exceptions.JAsyncCompositeException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class AnyPromiseTask<T> implements Task<T> {

    private final List<JPromise<? extends T>> promises;
    private final Map<JPromise<?>, Throwable> errors;
    @SuppressWarnings("unused")
    private volatile boolean resolved;
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<AnyPromiseTask, Boolean> RESOLVED_UPDATER = AtomicReferenceFieldUpdater.newUpdater(AnyPromiseTask.class, Boolean.TYPE, "resolved");
    private volatile int errorNum;
    @SuppressWarnings("rawtypes")
    private static final AtomicIntegerFieldUpdater<AnyPromiseTask> ERROR_NUM_UPDATER = AtomicIntegerFieldUpdater.newUpdater(AnyPromiseTask.class, "errorNum");

    public AnyPromiseTask(List<JPromise<? extends T>> promises) {
        this.promises = promises;
        this.errors = new ConcurrentHashMap<>();
    }

    @Override
    public void schedule(JThunk<T> thunk, JContext context) {
        for (JPromise<? extends T> promise : promises) {
            promise.onSuccess((v, ctx) -> {
                if (RESOLVED_UPDATER.compareAndSet(this, false, true)) {
                    for (JPromise<? extends T> promise2 : promises) {
                        if (promise2 != promise) {
                            promise2.cancel();
                        }
                    }
                    thunk.resolve(v, ctx);
                }
            }).onError((error, ctx) -> {
                int errorNum = ERROR_NUM_UPDATER.incrementAndGet(this);
                errors.put(promise, error);
                if (errorNum == promises.size()) {
                    thunk.reject(new JAsyncCompositeException(errors), ctx);
                }
            }).async(context);
        }
    }

    @Override
    public void cancel() {
        for (JPromise<? extends T> promise : promises) {
            promise.cancel();
        }
    }
}
