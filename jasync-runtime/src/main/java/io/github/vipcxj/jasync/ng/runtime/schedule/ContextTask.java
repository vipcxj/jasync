package io.github.vipcxj.jasync.ng.runtime.schedule;

import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JThunk;

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
