package io.github.vipcxj.jasync.runtime.schedule;

import io.github.vipcxj.jasync.spec.JContext;
import io.github.vipcxj.jasync.spec.JThunk;

public class ErrorTask<T> implements Task<T> {

    private final Throwable error;

    public ErrorTask(Throwable error) {
        this.error = error;
    }

    @Override
    public void schedule(JThunk<T> thunk, JContext context) {
        thunk.reject(error, context);
    }

    @Override
    public void cancel() { }
}
