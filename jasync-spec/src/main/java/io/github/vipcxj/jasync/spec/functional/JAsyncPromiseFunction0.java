package io.github.vipcxj.jasync.spec.functional;

import io.github.vipcxj.jasync.spec.JPromise2;

public interface JAsyncPromiseFunction0<T, R> {
    JPromise2<R> apply(T t) throws Throwable;
}
