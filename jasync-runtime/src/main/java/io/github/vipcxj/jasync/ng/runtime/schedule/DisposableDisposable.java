package io.github.vipcxj.jasync.ng.runtime.schedule;

import io.github.vipcxj.jasync.ng.spec.JDisposable;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class DisposableDisposable implements JDisposable {

    private final Future<JDisposable> future;
    private boolean disposed;

    public DisposableDisposable(Future<JDisposable> future) {
        this.future = future;
        this.disposed = false;
    }

    public void dispose(int times) {
        if (times == 0) {
            disposed = true;
        }
        if (future.isDone()) {
            try {
                JDisposable disposable = future.get();
                if (disposable != null) {
                    disposable.dispose();
                }
            } catch (InterruptedException | ExecutionException ignored) { }
            disposed = true;
        } else {
            future.cancel(true);
            if (!future.isCancelled()) {
                dispose(times - 1);
            } else {
                disposed = true;
            }
        }
    }

    @Override
    public void dispose() {
        dispose(3);
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }
}
