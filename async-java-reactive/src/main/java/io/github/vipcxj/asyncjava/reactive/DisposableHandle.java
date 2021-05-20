package io.github.vipcxj.asyncjava.reactive;

import io.github.vipcxj.asyncjava.Handle;
import reactor.core.Disposable;

public class DisposableHandle implements Handle {

    private final Disposable disposable;

    public DisposableHandle(Disposable disposable) {
        this.disposable = disposable;
    }

    @Override
    public void cancel() {
        disposable.dispose();
    }

    @Override
    public boolean isCanceled() {
        return disposable.isDisposed();
    }
}
