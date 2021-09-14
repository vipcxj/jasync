package io.github.vipcxj.jasync.spec.functional;

import io.github.vipcxj.jasync.spec.JPromise;

public interface LongVoidPromiseFunction {

    JPromise<Void> apply(long t) throws Throwable;
}
