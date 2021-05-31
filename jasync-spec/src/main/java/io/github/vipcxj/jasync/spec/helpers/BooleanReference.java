package io.github.vipcxj.jasync.spec.helpers;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;

public class BooleanReference implements Comparable<BooleanReference>, Serializable {

    private static final long serialVersionUID = 7295142474578239797L;
    private final AtomicBoolean atomic;

    public BooleanReference() {
        this(false);
    }

    public BooleanReference(boolean v) {
        this.atomic = new AtomicBoolean(v);
    }

    public boolean getValue() {
        return atomic.get();
    }

    public void setValue(boolean v) {
        atomic.set(v);
    }

    public boolean setAndGet(boolean v) {
        setValue(v);
        return v;
    }

    @Override
    public int compareTo(BooleanReference o) {
        boolean x = getValue();
        boolean y = getValue();
        return (x == y) ? 0 : (x ? 1 : -1);
    }
}
