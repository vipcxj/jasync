package io.github.vipcxj.jasync.spec.spi;

import io.github.vipcxj.jasync.spec.Promise;
import io.github.vipcxj.jasync.spec.functional.PromiseSupplier;

public interface PromiseProvider {

    <T> Promise<T> just(T value);

    <T> Promise<T> defer(PromiseSupplier<T> block);

    <T> Promise<T> error(Throwable t);
}
