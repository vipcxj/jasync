package io.github.vipcxj.jasync.spec.functional;

import io.github.vipcxj.jasync.spec.JPromise;

public interface IntVoidPromiseFunction {

    JPromise<Void> apply(int t) throws Throwable;
}
