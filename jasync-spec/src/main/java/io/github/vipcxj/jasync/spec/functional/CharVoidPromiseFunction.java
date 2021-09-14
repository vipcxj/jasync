package io.github.vipcxj.jasync.spec.functional;

import io.github.vipcxj.jasync.spec.JPromise;

public interface CharVoidPromiseFunction {

    JPromise<Void> apply(char t) throws Throwable;
}
