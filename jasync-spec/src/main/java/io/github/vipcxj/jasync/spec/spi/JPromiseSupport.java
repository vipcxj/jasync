package io.github.vipcxj.jasync.spec.spi;

import io.github.vipcxj.jasync.spec.JPromise2;
import io.github.vipcxj.jasync.spec.functional.JAsyncPortalTask;

import java.util.concurrent.TimeUnit;

public interface JPromiseSupport extends PrioritySupport {
    <T> JPromise2<T> just(T value);
    <T> JPromise2<T> error(Throwable error);
    JPromise2<Void> sleep(long time, TimeUnit unit);
    <T> JPromise2<T> portal(JAsyncPortalTask<T> task);
}
