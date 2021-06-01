package io.github.vipcxj.jasync.spec.functional;

import io.github.vipcxj.jasync.spec.Promise;

public interface PromiseSupplier<T> {

    Promise<T> get() throws Throwable;
}
