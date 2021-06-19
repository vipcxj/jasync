package io.github.vipcxj.jasync.spec.switchexpr;

import io.github.vipcxj.jasync.spec.functional.VoidPromiseSupplier;

public class StringCase implements ICase<String> {
    private final String cond;
    private final VoidPromiseSupplier body;

    public StringCase(String cond, VoidPromiseSupplier body) {
        this.cond = cond;
        this.body = body;
    }

    public String getCond() {
        return cond;
    }

    @Override
    public VoidPromiseSupplier getBody() {
        return body;
    }

    @Override
    public boolean is(String v, boolean findingDefault) {
        if (findingDefault)
            return false;
        if (cond == null) {
            return v == null;
        }
        return cond.equals(v);
    }

    public static StringCase of(String cond, VoidPromiseSupplier body) {
        return new StringCase(cond, body);
    }
}
