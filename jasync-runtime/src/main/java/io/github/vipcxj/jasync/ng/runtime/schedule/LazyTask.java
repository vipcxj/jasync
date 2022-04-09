package io.github.vipcxj.jasync.ng.runtime.schedule;

import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JDisposable;
import io.github.vipcxj.jasync.ng.spec.JScheduler;
import io.github.vipcxj.jasync.ng.spec.JThunk;

import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class LazyTask<T> implements Task<T> {

    private boolean disposed;
    private final long delay;
    private final TimeUnit timeUnit;
    private final BiConsumer<JThunk<T>, JContext> handler;
    private volatile JDisposable disposable;

    public LazyTask(BiConsumer<JThunk<T>, JContext> handler) {
        this(handler, 0, TimeUnit.MILLISECONDS);
    }

    public LazyTask(BiConsumer<JThunk<T>, JContext> handler, long delay, TimeUnit timeUnit) {
        this.handler = handler;
        this.delay = delay;
        this.timeUnit = timeUnit;
        this.disposed = false;
    }

    private void doSchedule(JThunk<T> thunk, JContext context) {
        try {
            handler.accept(thunk, context);
        } catch (Throwable t) {
            thunk.reject(t, context);
        }
    }

    @Override
    public synchronized void schedule(JThunk<T> thunk, JContext context) {
        if (!disposed) {
            JScheduler scheduler = context.getScheduler();
            if (delay > 0) {
                if (scheduler.supportDelay()) {
                    disposable = scheduler.schedule(() -> doSchedule(thunk, context), delay, timeUnit);
                } else {
                    FutureTask<JDisposable> futureTask = new FutureTask<>(() -> {
                        try {
                            timeUnit.sleep(delay);
                            return scheduler.schedule(() -> doSchedule(thunk, context));
                        } catch (InterruptedException ignored) {
                            return null;
                        }
                    });
                    new Thread(futureTask).start();
                    disposable = new DisposableDisposable(futureTask);
                }
            } else {
                disposable = scheduler.schedule(() -> doSchedule(thunk, context));
            }
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
