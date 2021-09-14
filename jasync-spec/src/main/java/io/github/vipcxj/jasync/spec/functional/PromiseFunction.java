package io.github.vipcxj.jasync.spec.functional;

import io.github.vipcxj.jasync.spec.JPromise;

public interface PromiseFunction<T, R> {

    JPromise<R> apply(T t) throws Throwable;
}
