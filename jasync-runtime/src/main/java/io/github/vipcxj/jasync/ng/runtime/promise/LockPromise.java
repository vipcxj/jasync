package io.github.vipcxj.jasync.ng.runtime.promise;

import io.github.vipcxj.jasync.ng.runtime.concurrent.ReentrantReadWriteLock;
import io.github.vipcxj.jasync.ng.runtime.concurrent.Waiter;
import io.github.vipcxj.jasync.ng.spec.JAsyncRoutine;
import io.github.vipcxj.jasync.ng.spec.JContext;

public class LockPromise extends AbstractPromise<Boolean> implements Waiter {

    private final boolean shared;
    private final boolean interruptible;
    private final long nanoTimeout;
    private JContext context;
    private final ReentrantReadWriteLock.Sync sync;

    public LockPromise(ReentrantReadWriteLock.Sync sync, boolean shared, boolean interruptible, long nanoTimeout) {
        super(null);
        this.sync = sync;
        this.shared = shared;
        this.interruptible = interruptible;
        this.nanoTimeout = nanoTimeout;
    }

    @Override
    protected void run(JContext context) {
        this.context = context;
        if (shared) {
            if (nanoTimeout > 0) {
                sync.tryAcquireSharedNanos(this, 1, nanoTimeout);
            } else {
                if (interruptible) {
                    sync.acquireSharedInterruptibly(this, 1);
                } else {
                    sync.acquireShared(this, 1);
                }
            }
        } else {
            if (nanoTimeout > 0) {
                sync.tryAcquireNanos(this, 1, nanoTimeout);
            } else {
                if (interruptible) {
                    sync.acquireInterruptibly(this, 1);
                } else {
                    sync.acquire(this, 1);
                }
            }
        }
    }

    @Override
    protected void dispose() {
        sync.interpret(this);
    }

    @Override
    public JAsyncRoutine getRoutine() {
        return context;
    }


    @Override
    public void resume(boolean locked) {
        assert context != null;
        resolve(locked, context);
    }

    @Override
    public void reject(Throwable t) {
        assert context != null;
        reject(t, context);
    }
}
