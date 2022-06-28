package io.github.vipcxj.jasync.ng.runtime.promise;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JPromiseTrigger;

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
    private static final int ST_RESOLVING = 1;
    private static final int ST_RESOLVED = 2;
    private static final int ST_REJECTING = 3;
    private static final int ST_REJECTED = 4;

    private volatile int tmState = 0;
    @SuppressWarnings("rawtypes")
    private static final AtomicIntegerFieldUpdater<PromiseTrigger> TM_STATE = AtomicIntegerFieldUpdater.newUpdater(PromiseTrigger.class, "tmState");
    private static final int ST_TM_VALID = 0;
    private static final int ST_TM_BUSY = 1;
    private static final int ST_TM_BUSY_INVALID = 2;
    private static final int ST_TM_INVALID = 3;

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
                    if (STATE.compareAndSet(this, ST_UNRESOLVED, ST_RESOLVING)) {
                        value = result;
                        state = ST_RESOLVED;
                        while (true) {
                            if (CB_STATE.weakCompareAndSet(this, ST_CB_SAFE, ST_CB_READING)) {
                                try {
                                    if (callbacks != null) {
                                        for (ThunkAndContext<T> callback : callbacks) {
                                            if (tmState == ST_TM_BUSY_INVALID) {
                                                return;
                                            }
                                            callback.getThunk().resolve(value, callback.getContext());
                                        }
                                        callbacks = null;
                                    }
                                } finally {
                                    CB_STATE.set(this, ST_CB_SAFE);
                                }
                                return;
                            } else if (tmState == ST_TM_BUSY_INVALID) {
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
                    if (STATE.compareAndSet(this, ST_UNRESOLVED, ST_REJECTING)) {
                        this.error = error;
                        state = ST_REJECTED;
                        while (true) {
                            if (CB_STATE.weakCompareAndSet(this, ST_CB_SAFE, ST_CB_READING)) {
                                try {
                                    if (callbacks != null) {
                                        for (ThunkAndContext<T> callback : callbacks) {
                                            if (tmState == ST_TM_BUSY_INVALID) {
                                                return;
                                            }
                                            callback.getThunk().reject(error, callback.getContext());
                                        }
                                        callbacks = null;
                                    }
                                } finally {
                                    CB_STATE.set(this, ST_CB_SAFE);
                                }
                                return;
                            } else if (tmState == ST_TM_BUSY_INVALID) {
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
                                    callback.getThunk().interrupt(callback.getContext());
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
            } else {
                TM_STATE.weakCompareAndSet(this, ST_TM_BUSY, ST_TM_BUSY_INVALID);
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
                thunk.interrupt(context);
            } else {
                while (true) {
                    if (TM_STATE.weakCompareAndSet(this, ST_TM_VALID, ST_TM_BUSY)) {
                        try {
                            while (true) {
                                if (CB_STATE.weakCompareAndSet(this, ST_CB_SAFE, ST_CB_PUTTING)) {
                                    try {
                                        if (callbacks == null) {
                                            callbacks = new ArrayList<>(1);
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
                                    thunk.interrupt(context);
                                    return;
                                }
                            }
                        } finally {
                            TM_STATE.set(this, ST_TM_VALID);
                        }
                    } else if (TM_STATE.get(this) == ST_TM_INVALID) {
                        thunk.interrupt(context);
                        return;
                    }
                }
            }
        });
    }
}
