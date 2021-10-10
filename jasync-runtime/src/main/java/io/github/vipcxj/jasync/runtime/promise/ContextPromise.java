package io.github.vipcxj.jasync.runtime.promise;

import io.github.vipcxj.jasync.runtime.schedule.ContextTask;
import io.github.vipcxj.jasync.spec.JContext;

public class ContextPromise extends BasePromise<JContext> {

    public ContextPromise() {
        super(new ContextTask(null));
    }

    public ContextPromise(JContext context) {
        super(new ContextTask(context));
    }
}
