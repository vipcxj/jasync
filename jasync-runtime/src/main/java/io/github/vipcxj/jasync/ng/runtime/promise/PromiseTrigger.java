package io.github.vipcxj.jasync.ng.runtime.promise;

import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JPromiseTrigger;
import io.github.vipcxj.jasync.ng.spec.JThunk;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class PromiseTrigger<T> implements JPromiseTrigger<T> {

    private T value;
    private Throwable error;
    private volatile int state = 0;
    @SuppressWarnings("rawtypes")
    private static final AtomicIntegerFieldUpdater<PromiseTrigger> STATE = AtomicIntegerFieldUpdater.newUpdater(PromiseTrigger.class, "state");
    private static final int ST_UNRESOLVED = 0;
    private static final int ST_RESOLVED = 1;
    private static final int ST_REJECTED = 2;

    private volatile int tmState = 0;
    @SuppressWarnings("rawtypes")
    private static final AtomicIntegerFieldUpdater<PromiseTrigger> TM_STATE = AtomicIntegerFieldUpdater.newUpdater(PromiseTrigger.class, "tmState");
    private static final int ST_TM_VALID = 0;
    private static final int ST_TM_BUSY = 1;
    private static final int ST_TM_INVALID = 2;

    private volatile int cbState = 0;
    @SuppressWarnings("rawtypes")
    private static final AtomicIntegerFieldUpdater<PromiseTrigger> CB_STATE = AtomicIntegerFieldUpdater.newUpdater(PromiseTrigger.class, "cbState");
    private static final int ST_CB_SAFE = 0;
    private static final int ST_CB_READING = 1;
    private static final int ST_CB_PUTTING = 2;

    private List<ThunkAndContext<T>> callbacks;

    public PromiseTrigger() { }

    @Override
    public void resolve(T result) {
        if (TM_STATE.get(this) == ST_TM_INVALID) {
            return;
        }
        while (true) {
            if (TM_STATE.weakCompareAndSet(this, ST_TM_VALID, ST_TM_BUSY)) {
                try {
                    if (STATE.compareAndSet(this, ST_UNRESOLVED, ST_RESOLVED)) {
                        value = result;
                        while (true) {
                            if (CB_STATE.weakCompareAndSet(this, ST_CB_SAFE, ST_CB_READING)) {
                                try {
                                    if (callbacks != null) {
                                        for (ThunkAndContext<T> callback : callbacks) {
                                            callback.thunk.resolve(value, callback.context);
                                        }
                                        callbacks = null;
                                    }
                                } finally {
                                    CB_STATE.set(this, ST_CB_SAFE);
                                }
                                return;
                            }
                        }
                    } else {
                        throw new IllegalStateException("The promise has been resolved or rejected.");
                    }
                } finally {
                    TM_STATE.set(this, ST_TM_VALID);
                }
            } else if (TM_STATE.get(this) == ST_TM_INVALID) {
                return;
            }
        }
    }

    @Override
    public void reject(Throwable error) {
        if (TM_STATE.get(this) == ST_TM_INVALID) {
            return;
        }
        while (true) {
            if (TM_STATE.weakCompareAndSet(this, ST_TM_VALID, ST_TM_BUSY)) {
                try {
                    if (STATE.compareAndSet(this, ST_UNRESOLVED, ST_REJECTED)) {
                        this.error = error;
                        while (true) {
                            if (CB_STATE.weakCompareAndSet(this, ST_CB_SAFE, ST_CB_READING)) {
                                try {
                                    if (callbacks != null) {
                                        for (ThunkAndContext<T> callback : callbacks) {
                                            callback.thunk.reject(error, callback.context);
                                        }
                                        callbacks = null;
                                    }
                                } finally {
                                    CB_STATE.set(this, ST_CB_SAFE);
                                }
                                return;
                            }
                        }
                    } else {
                        throw new IllegalStateException("The promise has been resolved or rejected.");
                    }
                } finally {
                    TM_STATE.set(this, ST_TM_VALID);
                }
            } else if (TM_STATE.get(this) == ST_TM_INVALID) {
                return;
            }
        }
    }

    @Override
    public void cancel() {
        if (TM_STATE.get(this) == ST_TM_INVALID) {
            return;
        }
        while (true) {
            if (TM_STATE.weakCompareAndSet(this, ST_TM_VALID, ST_TM_INVALID)) {
                while (true) {
                    if (CB_STATE.weakCompareAndSet(this, ST_CB_SAFE, ST_CB_READING)) {
                        try {
                            if (callbacks != null) {
                                for (ThunkAndContext<T> callback : callbacks) {
                                    callback.thunk.cancel();
                                }
                                callbacks = null;
                            }
                        } finally {
                            CB_STATE.set(this, ST_CB_SAFE);
                        }
                        return;
                    }
                }
            } else if (TM_STATE.get(this) == ST_TM_INVALID) {
                return;
            }
        }
    }

    @Override
    public JPromise<T> getPromise() {
        return JPromise.generate((thunk, context) -> {
            if (STATE.get(this) == ST_RESOLVED) {
                thunk.resolve(value, context);
            } else if (STATE.get(this) == ST_REJECTED) {
                thunk.reject(error, context);
            } else if (TM_STATE.get(this) == ST_TM_INVALID) {
                thunk.cancel();
            } else {
                while (true) {
                    if (TM_STATE.weakCompareAndSet(this, ST_TM_VALID, ST_TM_BUSY)) {
                        try {
                            while (true) {
                                if (CB_STATE.weakCompareAndSet(this, ST_CB_SAFE, ST_CB_PUTTING)) {
                                    try {
                                        if (callbacks == null) {
                                            callbacks = new ArrayList<>();
                                        }
                                        callbacks.add(new ThunkAndContext<>(thunk, context));
                                    } finally {
                                        CB_STATE.set(this, ST_CB_SAFE);
                                    }
                                    return;
                                } else if (STATE.get(this) == ST_RESOLVED) {
                                    thunk.resolve(value, context);
                                    return;
                                } else if (STATE.get(this) == ST_REJECTED) {
                                    thunk.reject(error, context);
                                    return;
                                } else if (TM_STATE.get(this) == ST_TM_INVALID) {
                                    thunk.cancel();
                                    return;
                                }
                            }
                        } finally {
                            TM_STATE.set(this, ST_TM_VALID);
                        }
                    } else if (TM_STATE.get(this) == ST_TM_INVALID) {
                        thunk.cancel();
                        return;
                    }
                }
            }
        });
    }

    static class ThunkAndContext<T> {
        final JThunk<T> thunk;
        final JContext context;

        ThunkAndContext(JThunk<T> thunk, JContext context) {
            this.thunk = thunk;
            this.context = context;
        }
    }
}
