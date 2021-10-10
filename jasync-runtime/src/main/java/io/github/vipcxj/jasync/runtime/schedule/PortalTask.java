package io.github.vipcxj.jasync.runtime.schedule;

import io.github.vipcxj.jasync.spec.*;
import io.github.vipcxj.jasync.spec.functional.JAsyncPortalTask;

public class PortalTask<T> implements Task<T> {

    private final JAsyncPortalTask<T> jumperTask;
    private JPortal<T> portal;
    private JDisposable disposable;
    private boolean disposed;


    public PortalTask(JAsyncPortalTask<T> jumperTask) {
        this.jumperTask = jumperTask;
        this.disposed = false;
    }

    public void bindPortal(JPortal<T> portal) {
        this.portal = portal;
    }

    @Override
    public void schedule(JThunk<T> thunk, JContext context) {
        if (!disposed) {
            this.disposable = context.getScheduler().schedule(() -> {
                try {
                    JPromise2<T> next = jumperTask.invoke(portal);
                    next = next != null ? next : JPromise2.empty();
                    next.onError(thunk::reject).onSuccess(thunk::resolve).async(context);
                } catch (Throwable t) {
                    thunk.reject(t, context);
                }
            });
        }
    }

    @Override
    public void cancel() {
        if (!disposed) {
            this.disposed = true;
            if (disposable != null) {
                disposable.dispose();
            }
        }
    }
}
