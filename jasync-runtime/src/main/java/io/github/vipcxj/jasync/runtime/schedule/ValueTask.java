package io.github.vipcxj.jasync.runtime.schedule;

import io.github.vipcxj.jasync.spec.JContext;
import io.github.vipcxj.jasync.spec.JThunk;

public class ValueTask<T> implements Task<T> {

    private final T value;

    public ValueTask(T value) {
        this.value = value;
    }

    @Override
    public void schedule(JThunk<T> thunk, JContext context) {
        thunk.resolve(value, context);
    }

    @Override
    public void cancel() { }
}
