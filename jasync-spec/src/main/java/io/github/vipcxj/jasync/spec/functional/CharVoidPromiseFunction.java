package io.github.vipcxj.jasync.spec.functional;

import io.github.vipcxj.jasync.spec.Promise;

public interface CharVoidPromiseFunction {

    Promise<Void> apply(char t) throws Throwable;
}
