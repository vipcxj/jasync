package io.github.vipcxj.jasync.spec.functional;

import io.github.vipcxj.jasync.spec.Promise;

public interface PromiseFunction<T, R> {

    Promise<R> apply(T t) throws Throwable;
}
