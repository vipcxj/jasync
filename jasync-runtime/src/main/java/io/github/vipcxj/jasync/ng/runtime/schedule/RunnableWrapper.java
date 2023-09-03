package io.github.vipcxj.jasync.ng.runtime.schedule;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import io.github.vipcxj.jasync.ng.spec.JDisposable;

public class RunnableWrapper implements Runnable, JDisposable {

    private final Runnable wrapped;
    private volatile boolean disposed;
    private volatile Thread workerThread;
    private static final AtomicReferenceFieldUpdater<RunnableWrapper, Thread> WORKER_THREAD_UPDATER = AtomicReferenceFieldUpdater.newUpdater(RunnableWrapper.class, Thread.class, "workerThread");

    public RunnableWrapper(Runnable wrapped) {
        this.wrapped = wrapped;
        this.disposed = false;
        this.workerThread = null;
    }

    public Runnable getWrapped() {
        return wrapped;
    }

    @Override
    public void run() {
        if (WORKER_THREAD_UPDATER.compareAndSet(this, null, Thread.currentThread())) {
            try {
                this.wrapped.run();
            } catch (Exception e) {
                Thread.interrupted();
                if (!(e instanceof InterruptedException)) {
                    throw e;
                }
            }
        }
    }

    @Override
    public void dispose() {
        while (this.workerThread == null) {
            if (WORKER_THREAD_UPDATER.compareAndSet(this, null, Thread.currentThread())) {
                this.disposed = true;
                return;
            }
        }
        try {
            this.workerThread.interrupt();
        } catch (SecurityException ignored) {

        } finally {
            this.disposed = true;
        }
    }

    @Override
    public boolean isDisposed() {
        return this.disposed;
    }
    
}
