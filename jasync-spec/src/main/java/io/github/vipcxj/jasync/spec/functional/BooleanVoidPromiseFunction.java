package io.github.vipcxj.jasync.spec.functional;

import io.github.vipcxj.jasync.spec.JPromise;

public interface BooleanVoidPromiseFunction {

    JPromise<Void> apply(boolean t) throws Throwable;
}
