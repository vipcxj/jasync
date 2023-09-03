package io.github.vipcxj.jasync.ng.runtime.schedule;

import io.github.vipcxj.jasync.ng.spec.JDisposable;
import io.github.vipcxj.schedule.EventHandle;
import io.github.vipcxj.schedule.Schedule;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class EventHandlerDisposable implements JDisposable {

    private static final EventHandle INVALID = Schedule.createInvalidHandle();
    private final JDisposable disposable;
    private volatile EventHandle handle;
    private static final AtomicReferenceFieldUpdater<EventHandlerDisposable, EventHandle> HANDLE
            = AtomicReferenceFieldUpdater.newUpdater(EventHandlerDisposable.class, EventHandle.class, "handle");

    public EventHandlerDisposable(JDisposable disposable) {
        this.disposable = disposable;
    }

    void updateHandle(EventHandle newHandle) {
        while (true) {
            EventHandle handle = this.handle;
            if (handle == INVALID) {
                newHandle.remove();
                disposable.dispose();
                return;
            }
            if (HANDLE.weakCompareAndSet(this, handle, newHandle)) {
                return;
            }
        }
    }

    @Override
    public void dispose() {
        while (true) {
            EventHandle handle = this.handle;
            if (handle == INVALID) {
                return;
            }
            if (HANDLE.weakCompareAndSet(this, handle, INVALID)) {
                handle.remove();
                disposable.dispose();
                return;
            }
        }
    }

    @Override
    public boolean isDisposed() {
        return handle == INVALID;
    }
}
