package io.github.vipcxj.jasync.spec.switchexpr;

import io.github.vipcxj.jasync.spec.functional.VoidPromiseSupplier;

public class DefaultCase<C> implements ICase<C> {

    private final VoidPromiseSupplier body;

    public DefaultCase(VoidPromiseSupplier body) {
        this.body = body;
    }

    @Override
    public boolean is(C v, boolean findingDefault) {
        return findingDefault;
    }

    @Override
    public VoidPromiseSupplier getBody() {
        return body;
    }

    public static <C> DefaultCase<C> of(VoidPromiseSupplier body) {
        return new DefaultCase<>(body);
    }
}
