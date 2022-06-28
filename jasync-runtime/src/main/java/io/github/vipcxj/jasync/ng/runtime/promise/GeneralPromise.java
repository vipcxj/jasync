package io.github.vipcxj.jasync.ng.runtime.promise;

import io.github.vipcxj.jasync.ng.runtime.schedule.Task;
import io.github.vipcxj.jasync.ng.spec.JContext;

public class GeneralPromise<T> extends AbstractPromise<T> {

    protected final Task<T> task;

    public GeneralPromise(Task<T> task, AbstractPromise<?> parent) {
        super(parent);
        this.task = task;
    }

    public GeneralPromise(Task<T> task) {
        this(task, null);
    }

    @Override
    protected void run(JContext context) {
        task.schedule(this, context);
    }

    @Override
    protected void dispose() {
        task.cancel();
    }
}
