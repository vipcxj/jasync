package io.github.vipcxj.jasync.ng.runtime.schedule;

import io.github.vipcxj.jasync.ng.spec.JDisposable;

import java.util.concurrent.Future;

public class FutureDisposable<V> implements JDisposable {

    private final Future<V> future;

    public FutureDisposable(Future<V> future) {
        this.future = future;
    }

    @Override
    public void dispose() {
        future.cancel(true);
    }

    @Override
    public boolean isDisposed() {
        return future.isCancelled();
    }
}
