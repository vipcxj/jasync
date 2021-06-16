package io.github.vipcxj.jasync.spec.functional;

import io.github.vipcxj.jasync.spec.Promise;

public interface DoubleVoidPromiseFunction {

    Promise<Void> apply(double t) throws Throwable;
}
