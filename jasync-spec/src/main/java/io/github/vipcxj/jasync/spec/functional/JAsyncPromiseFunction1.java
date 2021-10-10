package io.github.vipcxj.jasync.spec.functional;

import io.github.vipcxj.jasync.spec.JContext;
import io.github.vipcxj.jasync.spec.JPromise2;

public interface JAsyncPromiseFunction1<T, R> {
    JPromise2<R> apply(T t, JContext context) throws Throwable;
}
