package io.github.vipcxj.jasync.runtime.schedule;

import io.github.vipcxj.jasync.spec.JContext;
import io.github.vipcxj.jasync.spec.JThunk;

import java.util.function.BiConsumer;

public class ImmediateTask<T> implements Task<T> {

    private final BiConsumer<JThunk<T>, JContext> handler;

    public ImmediateTask(BiConsumer<JThunk<T>, JContext> handler) {
        this.handler = handler;
    }

    @Override
    public void schedule(JThunk<T> thunk, JContext context) {
        try {
            handler.accept(thunk, context);
        } catch (Throwable t) {
            thunk.reject(t, context);
        }
    }

    @Override
    public void cancel() { }
}
