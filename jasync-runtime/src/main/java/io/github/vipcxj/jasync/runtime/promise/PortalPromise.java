package io.github.vipcxj.jasync.runtime.promise;

import io.github.vipcxj.jasync.runtime.schedule.PortalTask;
import io.github.vipcxj.jasync.spec.JContext;
import io.github.vipcxj.jasync.spec.JPortal;
import io.github.vipcxj.jasync.spec.JPromise2;
import io.github.vipcxj.jasync.spec.functional.JAsyncPortalTask;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
        public JPromise2<T> jump() {
            return new Portal<>(source, source.jumperTask);
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
        public JPromise2<T> onSuccess(BiConsumer<T, JContext> resolver) {
            return this;
        }

        @Override
        public JPromise2<T> onError(BiConsumer<Throwable, JContext> reject) {
            return this;
        }

        @Override
        public JPromise2<T> onFinally(Consumer<JContext> consumer) {
            return this;
        }

        @Override
        public JPromise2<T> onDispose(Runnable runnable) {
            return this;
        }
    }
}
