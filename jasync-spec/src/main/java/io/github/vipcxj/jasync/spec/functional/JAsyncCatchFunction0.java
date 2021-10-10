package io.github.vipcxj.jasync.spec.functional;

import io.github.vipcxj.jasync.spec.JPromise2;

public interface JAsyncCatchFunction0<T extends Throwable, R> {
    JPromise2<R> apply(T error) throws Throwable;
}
