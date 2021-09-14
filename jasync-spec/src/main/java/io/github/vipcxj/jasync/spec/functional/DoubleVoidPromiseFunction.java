package io.github.vipcxj.jasync.spec.functional;

import io.github.vipcxj.jasync.spec.JPromise;

public interface DoubleVoidPromiseFunction {

    JPromise<Void> apply(double t) throws Throwable;
}
