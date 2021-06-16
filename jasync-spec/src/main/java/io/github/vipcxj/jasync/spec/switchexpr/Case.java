package io.github.vipcxj.jasync.spec.switchexpr;

import io.github.vipcxj.jasync.spec.functional.VoidPromiseSupplier;

public interface Case<T> {

    boolean is(T v);
    VoidPromiseSupplier getBody();
}
