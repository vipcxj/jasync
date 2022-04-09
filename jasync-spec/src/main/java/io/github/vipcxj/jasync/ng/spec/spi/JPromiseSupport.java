package io.github.vipcxj.jasync.ng.spec.spi;

import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JThunk;
import io.github.vipcxj.jasync.ng.spec.functional.JAsyncPortalTask1;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public interface JPromiseSupport extends PrioritySupport {
    <T> JPromise<T> just(T value);
    <T> JPromise<T> error(Throwable error);
    JPromise<Void> sleep(long time, TimeUnit unit);
    <T> JPromise<T> portal(JAsyncPortalTask1<T> task);
    <T> JPromise<T> any(List<JPromise<? extends T>> promises);
    @SuppressWarnings("unchecked")
    default <T> JPromise<T> any(JPromise<? extends T>... promises) {
        return any(Arrays.asList(promises));
    }
    <T> JPromise<List<T>> all(List<JPromise<? extends T>> promises);
    @SuppressWarnings("unchecked")
    default <T> JPromise<List<T>> all(JPromise<? extends T>... promises) {
        return all(Arrays.asList(promises));
    }
    <T> JPromise<T> create(BiConsumer<JThunk<T>, JContext> handler);
    <T> JPromise<T> generate(BiConsumer<JThunk<T>, JContext> handler);
}
