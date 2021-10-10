package io.github.vipcxj.jasync.spec.functional;

import io.github.vipcxj.jasync.spec.JContext;
import io.github.vipcxj.jasync.spec.JPromise2;

public interface JAsyncCatchFunction1<T extends Throwable, R> {
    JPromise2<R> apply(T error, JContext context) throws Throwable;
}
