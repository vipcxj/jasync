package io.github.vipcxj.jasync.spec.functional;

import io.github.vipcxj.jasync.spec.JPromise;

public interface ShortVoidPromiseFunction {

    JPromise<Void> apply(short t) throws Throwable;
}
