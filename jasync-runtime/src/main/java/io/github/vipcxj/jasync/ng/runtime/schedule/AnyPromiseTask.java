package io.github.vipcxj.jasync.ng.runtime.schedule;

import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JThunk;
import io.github.vipcxj.jasync.ng.spec.exceptions.JAsyncCompositeException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class AnyPromiseTask<T> implements Task<T> {

    private final List<JPromise<? extends T>> promises;
    private final List<Throwable> errors;
    @SuppressWarnings("unused")
    private volatile int resolved = 0;
    @SuppressWarnings("rawtypes")
    private static final AtomicIntegerFieldUpdater<AnyPromiseTask> RESOLVED_UPDATER = AtomicIntegerFieldUpdater.newUpdater(AnyPromiseTask.class, "resolved");
    private volatile int errorNum = 0;
    @SuppressWarnings("rawtypes")
    private static final AtomicIntegerFieldUpdater<AnyPromiseTask> ERROR_NUM_UPDATER = AtomicIntegerFieldUpdater.newUpdater(AnyPromiseTask.class, "errorNum");

    public AnyPromiseTask(List<JPromise<? extends T>> promises) {
        this.promises = promises;
        this.errors = new ArrayList<>(promises.size());
        for (int i = 0; i < promises.size(); ++i) {
            this.errors.add(i, null);
        }
    }

    @Override
    public void schedule(JThunk<T> thunk, JContext context) {
        int i = 0;
        for (JPromise<? extends T> promise : promises) {
            int finalI = i;
            promise.onSuccess((v, ctx) -> {
                if (RESOLVED_UPDATER.compareAndSet(this, 0, 1)) {
                    for (JPromise<? extends T> promise2 : promises) {
                        if (promise2 != promise) {
                            promise2.cancel();
                        }
                    }
                    thunk.resolve(v, ctx);
                }
            }).onError((error, ctx) -> {
                int errorNum = ERROR_NUM_UPDATER.incrementAndGet(this);
                errors.set(finalI, error);
                if (errorNum == promises.size()) {
                    thunk.reject(new JAsyncCompositeException(errors), ctx);
                }
            }).onCanceled((e, ctx) -> {
                int errorNum = ERROR_NUM_UPDATER.incrementAndGet(this);
                errors.set(finalI, e);
                if (errorNum == promises.size()) {
                    thunk.reject(new JAsyncCompositeException(errors), ctx);
                }
            }).async(context);
            ++i;
        }
    }

    @Override
    public void cancel() {
        for (JPromise<? extends T> promise : promises) {
            promise.cancel();
        }
    }
}
