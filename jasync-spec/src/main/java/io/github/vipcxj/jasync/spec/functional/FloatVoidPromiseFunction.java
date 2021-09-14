package io.github.vipcxj.jasync.spec.functional;

import io.github.vipcxj.jasync.spec.JPromise;

public interface FloatVoidPromiseFunction {

    JPromise<Void> apply(float t) throws Throwable;
}
