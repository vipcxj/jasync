package io.github.vipcxj.jasync.ng.spec.functional;

import io.github.vipcxj.jasync.ng.spec.JPromise;

public interface JAsyncCatchFunction0<T extends Throwable, R> {
    JPromise<R> apply(T error) throws Throwable;
}
