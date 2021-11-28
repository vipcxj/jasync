package io.github.vipcxj.jasync.runtime.promise;

import com.google.auto.service.AutoService;
import io.github.vipcxj.jasync.runtime.schedule.DelayTask;
import io.github.vipcxj.jasync.runtime.schedule.ImmediateTask;
import io.github.vipcxj.jasync.runtime.schedule.LazyTask;
import io.github.vipcxj.jasync.spec.JContext;
import io.github.vipcxj.jasync.spec.JPromise2;
import io.github.vipcxj.jasync.spec.JThunk;
import io.github.vipcxj.jasync.spec.functional.JAsyncPortalTask1;
import io.github.vipcxj.jasync.spec.spi.JPromiseSupport;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

@AutoService(JPromiseSupport.class)
public class PromiseSupport implements JPromiseSupport {

    @Override
    public <T> JPromise2<T> just(T value) {
        return new ValuePromise<>(value);
    }

    @Override
    public <T> JPromise2<T> error(Throwable error) {
        return new ErrorPromise<>(error);
    }

    @Override
    public JPromise2<Void> sleep(long time, TimeUnit unit) {
        return new BasePromise<>(new DelayTask(time, unit));
    }

    @Override
    public <T> JPromise2<T> portal(JAsyncPortalTask1<T> task) {
        return new PortalPromise<>(task);
    }

    @Override
    public <T> JPromise2<T> any(List<JPromise2<? extends T>> promises) {
        return new AnyPromise<>(promises);
    }

    @Override
    public <T> JPromise2<List<T>> all(List<JPromise2<? extends T>> promises) {
        return new AllPromise<>(promises);
    }

    @Override
    public <T> JPromise2<T> create(BiConsumer<JThunk<T>, JContext> handler) {
        return new BasePromise<>(new LazyTask<>(handler));
    }

    @Override
    public <T> JPromise2<T> generate(BiConsumer<JThunk<T>, JContext> handler) {
        return new BasePromise<>(new ImmediateTask<>(handler));
    }
}
