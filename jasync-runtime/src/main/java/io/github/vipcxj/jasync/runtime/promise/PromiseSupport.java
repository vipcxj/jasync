package io.github.vipcxj.jasync.runtime.promise;

import com.google.auto.service.AutoService;
import io.github.vipcxj.jasync.runtime.schedule.DelayTask;
import io.github.vipcxj.jasync.spec.JPromise2;
import io.github.vipcxj.jasync.spec.functional.JAsyncPortalTask;
import io.github.vipcxj.jasync.spec.spi.JPromiseSupport;

import java.util.concurrent.TimeUnit;

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
    public <T> JPromise2<T> portal(JAsyncPortalTask<T> task) {
        return new PortalPromise<>(task);
    }
}
