package io.github.vipcxj.jasync.ng.runtime.promise;

import com.google.auto.service.AutoService;
import io.github.vipcxj.jasync.ng.runtime.schedule.DelayTask;
import io.github.vipcxj.jasync.ng.runtime.schedule.ImmediateTask;
import io.github.vipcxj.jasync.ng.runtime.schedule.LazyTask;
import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JPromiseTrigger;
import io.github.vipcxj.jasync.ng.spec.JThunk;
import io.github.vipcxj.jasync.ng.spec.functional.JAsyncPortalTask1;
import io.github.vipcxj.jasync.ng.spec.spi.JPromiseSupport;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

@AutoService(JPromiseSupport.class)
public class PromiseSupport implements JPromiseSupport {

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
        return new BasePromise<>(new DelayTask(time, unit));
    }

    @Override
    public <T> JPromise<T> portal(JAsyncPortalTask1<T> task) {
        return new PortalPromise<>(task);
    }

    @Override
    public <T> JPromise<T> any(List<JPromise<? extends T>> promises) {
        return new AnyPromise<>(promises);
    }

    @Override
    public <T> JPromise<T> race(List<JPromise<? extends T>> promises) {
        return new RacePromise<>(promises);
    }

    @Override
    public <T> JPromise<List<T>> all(List<JPromise<? extends T>> promises) {
        return new AllPromise<>(promises);
    }

    @Override
    public <T> JPromise<T> create(BiConsumer<JThunk<T>, JContext> handler) {
        return new BasePromise<>(new LazyTask<>(handler));
    }

    @Override
    public <T> JPromise<T> generate(BiConsumer<JThunk<T>, JContext> handler) {
        return new BasePromise<>(new ImmediateTask<>(handler));
    }

    @Override
    public <T> JPromiseTrigger<T> createTrigger() {
        return new PromiseTrigger<>();
    }
}
