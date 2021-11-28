package io.github.vipcxj.jasync.spec.spi;

import io.github.vipcxj.jasync.spec.JContext;
import io.github.vipcxj.jasync.spec.JPromise2;
import io.github.vipcxj.jasync.spec.JThunk;
import io.github.vipcxj.jasync.spec.functional.JAsyncPortalTask1;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public interface JPromiseSupport extends PrioritySupport {
    <T> JPromise2<T> just(T value);
    <T> JPromise2<T> error(Throwable error);
    JPromise2<Void> sleep(long time, TimeUnit unit);
    <T> JPromise2<T> portal(JAsyncPortalTask1<T> task);
    <T> JPromise2<T> any(List<JPromise2<? extends T>> promises);
    default <T> JPromise2<T> any(JPromise2<? extends T>... promises) {
        return any(Arrays.asList(promises));
    }
    <T> JPromise2<List<T>> all(List<JPromise2<? extends T>> promises);
    default <T> JPromise2<List<T>> all(JPromise2<? extends T>... promises) {
        return all(Arrays.asList(promises));
    }
    <T> JPromise2<T> create(BiConsumer<JThunk<T>, JContext> handler);
    <T> JPromise2<T> generate(BiConsumer<JThunk<T>, JContext> handler);
}
