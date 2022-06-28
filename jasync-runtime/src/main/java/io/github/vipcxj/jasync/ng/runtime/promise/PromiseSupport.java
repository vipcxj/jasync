package io.github.vipcxj.jasync.ng.runtime.promise;

import com.google.auto.service.AutoService;
import io.github.vipcxj.jasync.ng.runtime.schedule.*;
import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JPromiseTrigger;
import io.github.vipcxj.jasync.ng.spec.JThunk;
import io.github.vipcxj.jasync.ng.spec.functional.JAsyncPortalTask1;
import io.github.vipcxj.jasync.ng.spec.spi.JPromiseSupport;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

@AutoService(JPromiseSupport.class)
public class PromiseSupport implements JPromiseSupport {

    private static final AtomicInteger CURRENT_ID = new AtomicInteger();

    @Override
    public <T> JPromise<T> just(T value) {
        return new ValuePromise<>(value);
    }

    @Override
    public <T> JPromise<T> error(Throwable error) {
        return new ErrorPromise<>(error);
    }

    @Override
    public JPromise<Void> sleep(long time, TimeUnit unit) {
        return new GeneralPromise<>(new DelayTask(time, unit));
    }

    @Override
    public <T> JPromise<T> portal(JAsyncPortalTask1<T> task) {
        return new PortalPromise<>(task);
    }

    @Override
    public <T> JPromise<T> any(List<JPromise<? extends T>> promises) {
        return new GeneralPromise<>(new AnyPromiseTask<>(promises));
    }

    @Override
    public <T> JPromise<T> race(List<JPromise<? extends T>> promises) {
        return new GeneralPromise<>(new RacePromiseTask<>(promises));
    }

    @Override
    public <T> JPromise<List<T>> all(List<JPromise<? extends T>> promises) {
        return new GeneralPromise<>(new AllPromisesTask<>(promises));
    }

    @Override
    public <T> JPromise<T> create(BiConsumer<JThunk<T>, JContext> handler) {
        return new GeneralPromise<>(new LazyTask<>(handler));
    }

    @Override
    public <T> JPromise<T> generate(BiConsumer<JThunk<T>, JContext> handler) {
        return new GeneralPromise<>(new ImmediateTask<>(handler));
    }

    @Override
    public <T> JPromiseTrigger<T> createTrigger() {
        return new PromiseTrigger<>();
    }

    @Override
    public int generateId() {
        return CURRENT_ID.getAndIncrement();
    }
}
