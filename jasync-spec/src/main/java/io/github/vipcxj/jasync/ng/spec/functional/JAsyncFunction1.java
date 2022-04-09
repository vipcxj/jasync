package io.github.vipcxj.jasync.ng.spec.functional;

import io.github.vipcxj.jasync.ng.spec.JContext;

public interface JAsyncFunction1<T, R> {
    R apply(T t, JContext context) throws Throwable;
}
