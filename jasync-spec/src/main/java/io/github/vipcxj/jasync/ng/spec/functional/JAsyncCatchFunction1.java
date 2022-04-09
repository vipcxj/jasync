package io.github.vipcxj.jasync.ng.spec.functional;

import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;

public interface JAsyncCatchFunction1<T extends Throwable, R> {
    JPromise<R> apply(T error, JContext context) throws Throwable;
}
