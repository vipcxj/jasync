package io.github.vipcxj.jasync.ng.spec.functional;

import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;

public interface JAsyncPromiseSupplier1<T> {
    JPromise<T> get(JContext context) throws Throwable;
}
