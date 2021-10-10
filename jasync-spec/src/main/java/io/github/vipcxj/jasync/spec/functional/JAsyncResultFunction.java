package io.github.vipcxj.jasync.spec.functional;

import io.github.vipcxj.jasync.spec.JPromise2;
import io.github.vipcxj.jasync.spec.JResult;

public interface JAsyncResultFunction<T, R> {
    JPromise2<R> apply(JResult<T> t) throws Throwable;
}
