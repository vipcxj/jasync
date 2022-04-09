package io.github.vipcxj.jasync.ng.runtime.schedule;

import io.github.vipcxj.jasync.ng.spec.*;
import io.github.vipcxj.jasync.ng.spec.functional.JAsyncPortalTask1;

public class PortalTask<T> implements Task<T> {

    private final JAsyncPortalTask1<T> jumperTask;
    private JPortal<T> portal;
    private JDisposable disposable;
    private boolean disposed;


    public PortalTask(JAsyncPortalTask1<T> jumperTask) {
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
                    JPromise<T> next = jumperTask.invoke(portal, context);
                    next = next != null ? next : JPromise.empty();
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
