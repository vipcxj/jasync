package io.github.vipcxj.jasync.ng.runtime.schedule;

import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JThunk;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class AllPromisesTask<T> implements Task<List<T>> {

    private final List<JPromise<? extends T>> promises;
    private final int num;
    private final Object[] values;
    private volatile int resolvedNum = 0;
    @SuppressWarnings("rawtypes")
    private static final AtomicIntegerFieldUpdater<AllPromisesTask> RESOLVED_NUM_UPDATER = AtomicIntegerFieldUpdater.newUpdater(AllPromisesTask.class, "resolvedNum");
    @SuppressWarnings("unused")
    private volatile int rejected = 0;
    @SuppressWarnings("rawtypes")
    private static final AtomicIntegerFieldUpdater<AllPromisesTask> REJECTED_UPDATER = AtomicIntegerFieldUpdater.newUpdater(AllPromisesTask.class, "rejected");

    public AllPromisesTask(List<JPromise<? extends T>> promises) {
        this.promises = promises;
        this.num = promises.size();
        this.values = new Object[this.num];
    }

    @Override
    public void schedule(JThunk<List<T>> thunk, JContext context) {
        int i = 0;
        for (JPromise<? extends T> promise : promises) {
            int index = i++;
            promise.onSuccess((v, ctx) -> {
                int resolvedNum = RESOLVED_NUM_UPDATER.incrementAndGet(this);
                values[index] = v;
                if (resolvedNum == num) {
                    List<T> result = new ArrayList<>(num);
                    for (Object value : values) {
                        //noinspection unchecked
                        result.add((T) value);
                    }
                    thunk.resolve(result, ctx);
                }
            }).onError((error, ctx) -> {
                if (REJECTED_UPDATER.compareAndSet(this, 0, 1)) {
                    for (JPromise<? extends T> promise2 : promises) {
                        if (promise2 != promise) {
                            promise2.cancel();
                        }
                    }
                    thunk.reject(error, ctx);
                }
            }).onCanceled((e, ctx) -> {
                if (REJECTED_UPDATER.compareAndSet(this, 0, 1)) {
                    for (JPromise<? extends T> promise2 : promises) {
                        if (promise2 != promise) {
                            promise2.cancel();
                        }
                    }
                    thunk.interrupt(e, ctx);
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
