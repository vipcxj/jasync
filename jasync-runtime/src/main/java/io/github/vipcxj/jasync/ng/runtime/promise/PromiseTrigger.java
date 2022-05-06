package io.github.vipcxj.jasync.ng.runtime.promise;

import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JPromiseTrigger;
import io.github.vipcxj.jasync.ng.spec.JThunk;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PromiseTrigger<T> implements JPromiseTrigger<T> {

    private T value;
    private Throwable error;
    private final AtomicInteger state;
    private final AtomicInteger cbState;
    private List<ThunkAndContext<T>> callbacks;
    private static final int ST_UNRESOLVED = 0;
    private static final int ST_RESOLVED = 1;
    private static final int ST_REJECTED = 2;
    private static final int ST_CANCELED = 3;

    private static final int ST_CB_SAFE = 0;
    private static final int ST_CB_READING = 1;
    private static final int ST_CB_PUTTING = 2;

    public PromiseTrigger() {
        this.state = new AtomicInteger(0);
        this.cbState = new AtomicInteger(0);
    }

    @Override
    public void resolve(T result) {
        if (state.compareAndSet(ST_UNRESOLVED, ST_RESOLVED)) {
            value = result;
            for (;;) {
                if (cbState.compareAndSet(ST_CB_SAFE, ST_CB_READING)) {
                    try {
                        if (callbacks != null) {
                            for (ThunkAndContext<T> callback : callbacks) {
                                callback.thunk.resolve(value, callback.context);
                            }
                            callbacks.clear();
                        }
                    } finally {
                        cbState.set(ST_CB_SAFE);
                    }
                    return;
                }
            }
        } else if (state.get() != ST_CANCELED) {
            throw new IllegalStateException("The promise has been resolved or rejected.");
        }
    }

    @Override
    public void reject(Throwable error) {
        if (state.compareAndSet(ST_UNRESOLVED, ST_REJECTED)) {
            this.error = error;
            for (;;) {
                if (cbState.compareAndSet(ST_CB_SAFE, ST_CB_READING)) {
                    try {
                        if (callbacks != null) {
                            for (ThunkAndContext<T> callback : callbacks) {
                                callback.thunk.reject(error, callback.context);
                            }
                            callbacks.clear();
                        }
                    } finally {
                        cbState.set(ST_CB_SAFE);
                    }
                    return;
                }
            }
        } else if (state.get() != ST_CANCELED) {
            throw new IllegalStateException("The promise has been resolved or rejected.");
        }
    }

    @Override
    public void cancel() {
        state.set(ST_CANCELED);
        for (;;) {
            if (cbState.compareAndSet(ST_CB_SAFE, ST_CB_READING)) {
                try {
                    if (callbacks != null) {
                        for (ThunkAndContext<T> callback : callbacks) {
                            callback.thunk.cancel();
                        }
                        callbacks.clear();
                    }
                } finally {
                    cbState.set(ST_CB_SAFE);
                }
                return;
            }
        }
    }

    @Override
    public JPromise<T> getPromise() {
        return JPromise.generate((thunk, context) -> {
            if (state.get() == ST_RESOLVED) {
                thunk.resolve(value, context);
            } else if (state.get() == ST_REJECTED) {
                thunk.reject(error, context);
            } else if (state.get() == ST_CANCELED) {
                thunk.cancel();
            } else {
                for (;;) {
                    try {
                        if (cbState.compareAndSet(ST_CB_SAFE, ST_CB_PUTTING)) {
                            if (callbacks == null) {
                                callbacks = new ArrayList<>();
                                callbacks.add(new ThunkAndContext<>(thunk, context));
                            }
                            return;
                        } else if (state.get() == ST_RESOLVED) {
                            thunk.resolve(value, context);
                            return;
                        } else if (state.get() == ST_REJECTED) {
                            thunk.reject(error, context);
                            return;
                        } else if (state.get() == ST_CANCELED) {
                            thunk.cancel();
                            return;
                        }
                    } finally {
                        cbState.set(ST_CB_SAFE);
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
