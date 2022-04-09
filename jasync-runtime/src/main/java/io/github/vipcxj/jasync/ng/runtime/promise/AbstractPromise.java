package io.github.vipcxj.jasync.ng.runtime.promise;

import io.github.vipcxj.jasync.ng.runtime.schedule.Task;
import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JThunk;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractPromise<T> implements JPromise<T>, JThunk<T> {
    protected boolean started;
    protected boolean resolved;
    protected boolean rejected;
    protected boolean disposed;
    protected List<JPromise<?>> children;
    protected List<Runnable> terminalHandlers;

    protected abstract Task<T> getTask();

    public AbstractPromise() {
        this.started = false;
        this.resolved = false;
        this.rejected = false;
        this.disposed = false;
    }

    @Override
    public void schedule(JContext context) {
        if (!disposed) {
            if (!started) {
                this.started = true;
                getTask().schedule(this, context);
            } else if (isCompleted()) {
                scheduleNext(context);
            }
        }
    }

    protected void scheduleNext(JContext context) {
        if (children != null) {
            for (JPromise<?> child : children) {
                child.schedule(context);
            }
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

    protected synchronized void markDisposed() {
        this.disposed = true;
        notifyAll();
    }

    protected List<JPromise<?>> getChildren() {
        if (children == null) {
            children = new ArrayList<>();
        }
        return children;
    }

    public List<Runnable> getTerminalHandlers() {
        if (terminalHandlers == null) {
            terminalHandlers = new ArrayList<>();
        }
        return terminalHandlers;
    }

    private void triggerTerminalHandlers() {
        if (terminalHandlers != null) {
            for (Runnable handler : terminalHandlers) {
                handler.run();
            }
        }
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
        markDisposed();
        getTask().cancel();
        try {
            triggerTerminalHandlers();
        } finally {
            if (children != null) {
                for (JPromise<?> child : children) {
                    child.dispose();
                }
            }
        }
    }
}
