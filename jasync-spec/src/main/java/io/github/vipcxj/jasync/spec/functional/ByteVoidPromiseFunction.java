package io.github.vipcxj.jasync.spec.functional;

import io.github.vipcxj.jasync.spec.Promise;

public interface ByteVoidPromiseFunction {

    Promise<Void> apply(byte t) throws Throwable;
}
