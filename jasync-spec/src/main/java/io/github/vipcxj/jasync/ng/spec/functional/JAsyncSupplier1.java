package io.github.vipcxj.jasync.ng.spec.functional;

import io.github.vipcxj.jasync.ng.spec.JContext;

public interface JAsyncSupplier1<R> {
    R get(JContext context) throws Throwable;
}
