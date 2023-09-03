package io.github.vipcxj.jasync.ng.runtime.schedule;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import io.github.vipcxj.jasync.ng.spec.JDisposable;

public class DisposableHandler implements JDisposable {

    private static final JDisposable FAKE_DISPOSABLE = new DummyDisposable();
    private volatile JDisposable disposable = null;
    private volatile boolean disposed = false;
    private static final AtomicReferenceFieldUpdater<DisposableHandler, JDisposable> DISPOSABLE_UPDATER = AtomicReferenceFieldUpdater.newUpdater(DisposableHandler.class, JDisposable.class, "disposable");


    static class DummyDisposable implements JDisposable {

        @Override
        public void dispose() {
            throw new UnsupportedOperationException("Unimplemented method 'dispose'");
        }

        @Override
        public boolean isDisposed() {
            throw new UnsupportedOperationException("Unimplemented method 'isDisposed'");
        }
    }

    public void updateDisposable(JDisposable disposable) {
        if (!DISPOSABLE_UPDATER.compareAndSet(this, null, disposable)) {
            if (this.disposable == FAKE_DISPOSABLE) {
                disposable.dispose();
            }
        }
    }

    @Override
    public void dispose() {
        while (this.disposable != FAKE_DISPOSABLE) {
            if (DISPOSABLE_UPDATER.compareAndSet(this, null, FAKE_DISPOSABLE)) {
                this.disposed = true;
                return;
            } else {
                JDisposable disposable = this.disposable;
                if (disposable != FAKE_DISPOSABLE) {
                    if (DISPOSABLE_UPDATER.compareAndSet(this, disposable, FAKE_DISPOSABLE)) {
                        disposable.dispose();
                        this.disposed = true;
                        return;
                    }
                }
            }
        }
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }
    
}
