package io.github.vipcxj.jasync.spec.functional;

import io.github.vipcxj.jasync.spec.JContext;
import io.github.vipcxj.jasync.spec.JPromise2;

public interface JAsyncPromiseSupplier1<T> {
    JPromise2<T> get(JContext context) throws Throwable;
}
