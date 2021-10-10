package io.github.vipcxj.jasync.runtime.promise;

import io.github.vipcxj.jasync.runtime.schedule.Task;
import io.github.vipcxj.jasync.spec.JContext;
import io.github.vipcxj.jasync.spec.JPromise2;
import io.github.vipcxj.jasync.spec.JThunk;

public abstract class AbstractPromise<T> implements JPromise2<T>, JThunk<T> {
    protected boolean started;
    protected boolean resolved;
    protected boolean rejected;
    protected boolean disposed;

    protected abstract Task<T> getTask();

    public AbstractPromise() {
        this.started = false;
        this.resolved = false;
        this.rejected = false;
        this.disposed = false;
    }

    @Override
    public void schedule(JContext context) {
        if (!started && !disposed) {
            this.started = true;
            getTask().schedule(this, context);
        }
    }

    protected synchronized void markResolved() {
        this.resolved = true;
        this.rejected = false;
        notifyAll();
    }

    protected synchronized void markRejected() {
        this.resolved = false;
        this.rejected = true;
        notifyAll();
    }

    @Override
    public boolean isResolved() {
        return resolved;
    }

    @Override
    public boolean isRejected() {
        return rejected;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        getTask().cancel();
    }
}
