package io.github.vipcxj.jasync.ng.spec.functional;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JResult;

public interface JAsyncResultFunction<T, R> {
    JPromise<R> apply(JResult<T> t) throws Throwable;
}
