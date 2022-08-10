package io.github.vipcxj.jasync.ng.runtime.schedule;

import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JThunk;
import io.github.vipcxj.schedule.EventHandle;
import io.github.vipcxj.schedule.Schedule;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class DelayTask implements Task<Void> {

    private final long timeout;
    private final TimeUnit unit;

    private volatile boolean disposed;
    private volatile EventHandle handle;
    private static final AtomicReferenceFieldUpdater<DelayTask, EventHandle> HANDLE
            = AtomicReferenceFieldUpdater.newUpdater(DelayTask.class, EventHandle.class, "handle");

    public DelayTask(long timeout, TimeUnit unit) {
        this.timeout = timeout;
        this.unit = unit;
    }

    private void dispose() {
        EventHandle handle = this.handle;
        if (handle != null && HANDLE.compareAndSet(this, handle, null)) {
            handle.remove();
        }
    }

    @Override
    public synchronized void schedule(JThunk<Void> thunk, JContext context) {
        if (!disposed) {
            handle = Schedule.instance().addEvent(timeout, unit, () -> {
                try {
                    thunk.resolve(null, context);
                } catch (Throwable t) {
                    thunk.reject(t, context);
                }
            });
            if (disposed) {
                dispose();
            }
        }
    }

    @Override
    public synchronized void cancel() {
        if (!disposed) {
            disposed = true;
            dispose();
        }
    }
}
