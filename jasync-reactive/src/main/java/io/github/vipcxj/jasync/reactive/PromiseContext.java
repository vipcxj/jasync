package io.github.vipcxj.jasync.reactive;

import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JThunk;

public class PromiseContext<T> {
    private final JThunk<T> thunk;
    private final JContext context;
    private final long nextRequest;

    public PromiseContext(JThunk<T> thunk, JContext context, long nextRequest) {
        this.thunk = thunk;
        this.context = context;
        this.nextRequest = nextRequest;
    }

    public JThunk<T> getThunk() {
        return thunk;
    }

    public JContext getContext() {
        return context;
    }

    public long getNextRequest() {
        return nextRequest;
    }
}
