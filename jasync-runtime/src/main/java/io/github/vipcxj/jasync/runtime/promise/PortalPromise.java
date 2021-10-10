package io.github.vipcxj.jasync.runtime.promise;

import io.github.vipcxj.jasync.runtime.schedule.PortalTask;
import io.github.vipcxj.jasync.spec.JContext;
import io.github.vipcxj.jasync.spec.JPortal;
import io.github.vipcxj.jasync.spec.JPromise2;
import io.github.vipcxj.jasync.spec.functional.JAsyncPortalTask;

public class PortalPromise<T> extends BasePromise<T> implements JPortal<T> {

    private final JAsyncPortalTask<T> jumperTask;

    public PortalPromise(JAsyncPortalTask<T> jumperTask) {
        super(new PortalTask<>(jumperTask));
        PortalTask<T> theTask = (PortalTask<T>) this.task;
        theTask.bindPortal(this);
        this.jumperTask = jumperTask;
    }

    @Override
    public JPromise2<T> jump() {
        return new Portal<>(this, jumperTask);
    }

    public static class Portal<T> extends PortalPromise<T> {

        private final PortalPromise<T> source;

        public Portal(PortalPromise<T> source, JAsyncPortalTask<T> jumperTask) {
            super(jumperTask);
            this.source = source;
        }

        @Override
        protected void resolve(T result, JContext context, boolean next) {
            source.resolve(result, context, next);
        }

        @Override
        protected void reject(Throwable error, JContext context, boolean next) {
            source.reject(error, context, next);
        }

        @Override
        public JPromise2<T> jump() {
            return new Portal<>(source, source.jumperTask);
        }
    }
}
