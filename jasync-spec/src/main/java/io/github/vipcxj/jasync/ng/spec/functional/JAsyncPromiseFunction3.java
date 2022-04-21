package io.github.vipcxj.jasync.ng.spec.functional;

import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;

public interface JAsyncPromiseFunction3<T, R> {
    JPromise<R> apply(T t, Throwable throwable, JContext context) throws Throwable;
}
