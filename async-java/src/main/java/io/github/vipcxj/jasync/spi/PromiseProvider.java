package io.github.vipcxj.jasync.spi;

import io.github.vipcxj.jasync.Promise;

import java.util.function.Supplier;

public interface PromiseProvider {

    <T> Promise<T> just(T value);

    <T> Promise<T> defer(Supplier<Promise<T>> block);
}
