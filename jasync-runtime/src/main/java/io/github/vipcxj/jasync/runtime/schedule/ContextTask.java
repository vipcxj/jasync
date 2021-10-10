package io.github.vipcxj.jasync.runtime.schedule;

import io.github.vipcxj.jasync.spec.JContext;
import io.github.vipcxj.jasync.spec.JThunk;

public class ContextTask implements Task<JContext> {

    private final JContext context;

    public ContextTask(JContext context) {
        this.context = context;
    }

    @Override
    public void schedule(JThunk<JContext> thunk, JContext context) {
        context = this.context != null ? this.context : context;
        thunk.resolve(context, context);
    }

    @Override
    public void cancel() {

    }
}
