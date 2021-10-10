package io.github.vipcxj.jasync.runtime.schedule;

import io.github.vipcxj.jasync.spec.JContext;
import io.github.vipcxj.jasync.spec.JDisposable;
import io.github.vipcxj.jasync.spec.JThunk;

import java.util.function.BiConsumer;

public class LazyTask<T> implements Task<T> {

    private boolean disposed;
    private final BiConsumer<JThunk<T>, JContext> handler;
    private volatile JDisposable disposable;

    public LazyTask(BiConsumer<JThunk<T>, JContext> handler) {
        this.handler = handler;
        this.disposed = false;
    }

    @Override
    public synchronized void schedule(JThunk<T> thunk, JContext context) {
        if (!disposed) {
            disposable = context.getScheduler().schedule(() -> {
                try {
                    handler.accept(thunk, context);
                } catch (Throwable t) {
                    thunk.reject(t, context);
                }
            });
        }
    }

    @Override
    public synchronized void cancel() {
        if (!disposed) {
            disposed = true;
            if (disposable != null) {
                disposable.dispose();
            }
        }
    }
}
