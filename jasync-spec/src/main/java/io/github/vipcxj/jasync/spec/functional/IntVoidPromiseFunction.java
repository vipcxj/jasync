package io.github.vipcxj.jasync.spec.functional;

import io.github.vipcxj.jasync.spec.Promise;

public interface IntVoidPromiseFunction {

    Promise<Void> apply(int t) throws Throwable;
}
