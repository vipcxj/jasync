package io.github.vipcxj.jasync.spec.functional;

import io.github.vipcxj.jasync.spec.Promise;

public interface FloatVoidPromiseFunction {

    Promise<Void> apply(float t) throws Throwable;
}
