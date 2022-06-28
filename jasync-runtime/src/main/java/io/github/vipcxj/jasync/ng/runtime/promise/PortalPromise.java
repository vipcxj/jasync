package io.github.vipcxj.jasync.ng.runtime.promise;

import io.github.vipcxj.jasync.ng.runtime.schedule.PortalTask;
import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JPortal;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.functional.JAsyncPortalTask1;
import io.github.vipcxj.jasync.ng.spec.functional.TriConsumer;

import java.util.function.BiConsumer;

public class PortalPromise<T> extends GeneralPromise<T> implements JPortal<T> {

    private final JAsyncPortalTask1<T> jumperTask;
    private boolean interrupted;

    public PortalPromise(JAsyncPortalTask1<T> jumperTask) {
        super(new PortalTask<>(jumperTask));
        PortalTask<T> theTask = (PortalTask<T>) this.task;
        theTask.bindPortal(this);
        this.jumperTask = jumperTask;
    }

    @Override
    public JPromise<T> jump() {
        return new Portal<>(this, jumperTask, 1);
    }

    @Override
    public void cancel() {
        super.cancel();
        interrupted = true;
    }

    @Override
    public long repeated() {
        return 0;
    }

    @Override
    public boolean isInterrupted() {
        return interrupted;
    }

    public static class Portal<T> extends PortalPromise<T> {

        private final PortalPromise<T> source;
        private final long repeated;

        public Portal(PortalPromise<T> source, JAsyncPortalTask1<T> jumperTask, long repeated) {
            super(jumperTask);
            this.source = source;
            this.repeated = repeated;
        }

        @Override
        public JPromise<T> jump() {
            return new Portal<>(source, source.jumperTask, repeated + 1);
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

        @Override
        public JPromise<T> onCanceled(BiConsumer<InterruptedException, JContext> callback) {
            return this;
        }

        @Override
        public long repeated() {
            return repeated;
        }

        @Override
        public boolean isInterrupted() {
            return source.interrupted;
        }
    }
}
