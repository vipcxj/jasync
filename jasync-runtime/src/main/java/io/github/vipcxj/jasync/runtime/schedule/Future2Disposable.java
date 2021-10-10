package io.github.vipcxj.jasync.runtime.schedule;

import io.github.vipcxj.jasync.spec.JDisposable;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Future2Disposable implements JDisposable {

    private final Future<Future<?>> futureFuture;

    public Future2Disposable(Future<Future<?>> futureFuture) {
        this.futureFuture = futureFuture;
    }

    public void dispose(int times) {
        if (times == 0) {
            return;
        }
        if (futureFuture.isDone()) {
            try {
                Future<?> future = futureFuture.get();
                if (future != null) {
                    future.cancel(true);
                }
            } catch (InterruptedException | ExecutionException ignored) { }
        } else {
            futureFuture.cancel(true);
            if (!futureFuture.isCancelled()) {
                new Thread(() -> {
                    try {
                        Thread.sleep(0);
                        dispose(times - 1);
                    } catch (InterruptedException ignored) {}
                }).start();
            }
        }
    }

    @Override
    public void dispose() {
        dispose(3);
    }

    @Override
    public boolean isDisposed() {
        return false;
    }
}
