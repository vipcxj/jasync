package io.github.vipcxj.jasync.ng.runtime.promise;

import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JThunk;

public class ThunkAndContext<T> {

    private final JThunk<T> thunk;
    private final JContext context;

    ThunkAndContext(JThunk<T> thunk, JContext context) {
        this.thunk = thunk;
        this.context = context;
    }

    public JThunk<T> getThunk() {
        return thunk;
    }

    public JContext getContext() {
        return context;
    }
}
