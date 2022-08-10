package io.github.vipcxj.jasync.ng.runtime.promise;

import io.github.vipcxj.jasync.ng.runtime.schedule.PortalTask;
import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JPortal;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.functional.JAsyncPortalTask1;
import io.github.vipcxj.jasync.ng.spec.functional.TriConsumer;

import java.util.function.BiConsumer;

public class PortalPromise<T> extends GeneralPromise<T> implements JPortal<T> {

    public PortalPromise(JAsyncPortalTask1<T> jumperTask) {
        this(new PortalTask<>(jumperTask), true);
    }

    private PortalPromise(PortalTask<T> portalTask, boolean bind) {
        super(portalTask);
        if (bind) {
            portalTask.bindPortal(this);
        }
    }

    @Override
    public JPromise<T> jump() {
        return new Portal<>(this, (PortalTask<T>) task);
    }

    @Override
    public void cancel() {
        super.cancel();
    }

    public static class Portal<T> extends PortalPromise<T> {

        private final PortalPromise<T> source;

        public Portal(PortalPromise<T> source, PortalTask<T> portalTask) {
            super(portalTask, false);
            this.source = source;
        }

        @Override
        public JPromise<T> jump() {
            return new Portal<>(source, (PortalTask<T>) task);
        }

        @Override
        public void resolve(T result, JContext context) {
            source.resolve(result, context);
        }

        @Override
        public void reject(Throwable error, JContext context) {
            source.reject(error, context);
        }

        @Override
        public JPromise<T> onSuccess(BiConsumer<T, JContext> callback) {
            return this;
        }

        @Override
        public JPromise<T> onError(BiConsumer<Throwable, JContext> callback) {
            return this;
        }

        @Override
        public JPromise<T> onFinally(TriConsumer<T, Throwable, JContext> callback) {
            return this;
        }
    }
}
