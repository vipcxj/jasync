package io.github.vipcxj.jasync.spec.functional;

import io.github.vipcxj.jasync.spec.JPromise;

public interface PromiseSupplier<T> {

    JPromise<T> get() throws Throwable;
}
