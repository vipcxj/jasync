package io.github.vipcxj.jasync.ng.spec.functional;

import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;

public interface JAsyncPromiseFunction1<T, R> {
    JPromise<R> apply(T t, JContext context) throws Throwable;
}
