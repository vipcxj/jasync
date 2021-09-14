package io.github.vipcxj.jasync.spec.functional;

import io.github.vipcxj.jasync.spec.JPromise;

public interface ByteVoidPromiseFunction {

    JPromise<Void> apply(byte t) throws Throwable;
}
