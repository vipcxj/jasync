package io.github.vipcxj.jasync.spec.spi;

import io.github.vipcxj.jasync.spec.JPromise;
import io.github.vipcxj.jasync.spec.functional.PromiseSupplier;

public interface PromiseProvider extends PrioritySupport {

    <T> JPromise<T> just(T value);

    <T> JPromise<T> defer(PromiseSupplier<T> block);

    <T> JPromise<T> error(Throwable t);
}
