package io.github.vipcxj.jasync.spec.switchexpr;

import io.github.vipcxj.jasync.spec.functional.VoidPromiseSupplier;

public interface ICase<T> {

    boolean is(T v, boolean findingDefault);
    VoidPromiseSupplier getBody();
}
