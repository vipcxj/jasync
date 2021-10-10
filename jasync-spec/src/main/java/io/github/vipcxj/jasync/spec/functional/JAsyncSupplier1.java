package io.github.vipcxj.jasync.spec.functional;

import io.github.vipcxj.jasync.spec.JContext;

public interface JAsyncSupplier1<R> {
    R get(JContext context) throws Throwable;
}
