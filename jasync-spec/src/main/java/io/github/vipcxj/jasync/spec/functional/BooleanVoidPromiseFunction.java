package io.github.vipcxj.jasync.spec.functional;

import io.github.vipcxj.jasync.spec.Promise;

public interface BooleanVoidPromiseFunction {

    Promise<Void> apply(boolean t) throws Throwable;
}
