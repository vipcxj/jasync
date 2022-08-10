package io.github.vipcxj.jasync.ng.runtime.schedule;

import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JDisposable;
import io.github.vipcxj.jasync.ng.spec.JScheduler;
import io.github.vipcxj.jasync.ng.spec.JThunk;
import io.github.vipcxj.schedule.EventHandle;
import io.github.vipcxj.schedule.Schedule;

import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BiConsumer;

public class LazyTask<T> implements Task<T> {

    private volatile boolean disposed;
    private final long delay;
    private final TimeUnit timeUnit;
    private final BiConsumer<JThunk<T>, JContext> handler;
    private volatile JDisposable disposable;
    @SuppressWarnings("rawtypes")
    private final static AtomicReferenceFieldUpdater<LazyTask, JDisposable> DISPOSABLE = AtomicReferenceFieldUpdater.newUpdater(LazyTask.class, JDisposable.class, "disposable");

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

    private void dispose() {
        JDisposable disposable = this.disposable;
        if (disposable != null && DISPOSABLE.compareAndSet(this, disposable, null)) {
            disposable.dispose();
        }
    }

    @Override
    public void schedule(JThunk<T> thunk, JContext context) {
        if (!disposed) {
            JScheduler scheduler = context.getScheduler();
            if (delay > 0) {
                if (scheduler.supportDelay()) {
                    disposable = scheduler.schedule(() -> doSchedule(thunk, context), delay, timeUnit);
                } else {
                    final EventHandlerDisposable disposable = new EventHandlerDisposable();
                    EventHandle handle = Schedule.instance().addEvent(delay, timeUnit, () -> {
                        scheduler.schedule(() -> doSchedule(thunk, context));
                    });
                    disposable.updateHandle(handle);
                    this.disposable = disposable;
                }
            } else {
                disposable = scheduler.schedule(() -> doSchedule(thunk, context));
            }
            if (disposed) {
                dispose();
            }
        }
    }

    @Override
    public void cancel() {
        if (!disposed) {
            disposed = true;
            dispose();
        }
    }
}
