package io.github.vipcxj.jasync.spec.spi;

import io.github.vipcxj.jasync.spec.Promise;

import java.util.function.Supplier;

public interface PromiseProvider {

    <T> Promise<T> just(T value);

    <T> Promise<T> defer(Supplier<Promise<T>> block);

    <T> Promise<T> error(Throwable t);
}
