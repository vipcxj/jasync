package io.github.vipcxj.jasync.spec.switchexpr;

import io.github.vipcxj.jasync.spec.functional.VoidPromiseSupplier;

public class EnumCase<E extends Enum<E>> implements ICase<Enum<E>> {
    private final E cond;
    private final VoidPromiseSupplier body;

    public EnumCase(E cond, VoidPromiseSupplier body) {
        this.cond = cond;
        this.body = body;
    }

    public E getCond() {
        return cond;
    }

    @Override
    public VoidPromiseSupplier getBody() {
        return body;
    }

    @Override
    public boolean is(Enum<E> v, boolean findingDefault) {
        return !findingDefault && v == cond;
    }

    public static <E extends Enum<E>> EnumCase<E> of(E cond, VoidPromiseSupplier body) {
        return new EnumCase<>(cond, body);
    }
}
