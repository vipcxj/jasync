package io.github.vipcxj.jasync.ng.spec.functional;

import io.github.vipcxj.jasync.ng.spec.JPromise;

public interface JAsyncPromiseFunction0<T, R> {
    JPromise<R> apply(T t) throws Throwable;
}
