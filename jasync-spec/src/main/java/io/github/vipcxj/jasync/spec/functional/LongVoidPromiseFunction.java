package io.github.vipcxj.jasync.spec.functional;

import io.github.vipcxj.jasync.spec.Promise;

public interface LongVoidPromiseFunction {

    Promise<Void> apply(long t) throws Throwable;
}
