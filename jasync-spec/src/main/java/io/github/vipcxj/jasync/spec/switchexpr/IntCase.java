package io.github.vipcxj.jasync.spec.switchexpr;

import io.github.vipcxj.jasync.spec.functional.VoidPromiseSupplier;

public class IntCase implements ICase<Integer> {
    private final int cond;
    private final VoidPromiseSupplier body;

    public IntCase(int cond, VoidPromiseSupplier body) {
        this.cond = cond;
        this.body = body;
    }

    public int getCond() {
        return cond;
    }

    @Override
    public VoidPromiseSupplier getBody() {
        return body;
    }

    public boolean is(int v, boolean findingDefault) {
        return !findingDefault && v == cond;
    }

    @Override
    public boolean is(Integer v, boolean findingDefault) {
        return !findingDefault && v != null && v == cond;
    }

    public static IntCase of(int cond, VoidPromiseSupplier body) {
        return new IntCase(cond, body);
    }
}
