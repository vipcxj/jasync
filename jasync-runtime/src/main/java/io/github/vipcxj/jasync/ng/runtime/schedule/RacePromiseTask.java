package io.github.vipcxj.jasync.ng.runtime.schedule;

import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JThunk;

import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class RacePromiseTask<T> implements Task<T> {

    private final List<JPromise<? extends T>> promises;
    /**
     * 0: init state;
     * 1: resolved;
     * -1: rejected;
     */
    private volatile int state;
    @SuppressWarnings("rawtypes")
    private final static AtomicIntegerFieldUpdater<RacePromiseTask> STATE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(RacePromiseTask.class, "state");

    public RacePromiseTask(List<JPromise<? extends T>> promises) {
        this.promises = promises;
        this.state = 0;
    }

    @Override
    public void schedule(JThunk<T> thunk, JContext context) {
        for (JPromise<? extends T> promise : promises) {
            promise.onSuccess((v, ctx) -> {
                if (STATE_UPDATER.compareAndSet(this, 0, 1)) {
                    thunk.resolve(v, ctx);
                    for (JPromise<? extends T> promise1 : promises) {
                        if (promise1 != promise) {
                            promise1.cancel();
                        }
                    }
                }
            }).onError((error, ctx) -> {
                if (STATE_UPDATER.compareAndSet(this, 0, -1)) {
                    thunk.reject(error, ctx);
                    for (JPromise<? extends T> promise1 : promises) {
                        if (promise1 != promise) {
                            promise1.cancel();
                        }
                    }
                }
            }).onCanceled((e, ctx) -> {
                if (STATE_UPDATER.compareAndSet(this, 0, -1)) {
                    thunk.interrupt(e, ctx);
                    for (JPromise<? extends T> promise1 : promises) {
                        if (promise1 != promise) {
                            promise1.cancel();
                        }
                    }
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
