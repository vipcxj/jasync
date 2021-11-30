package io.github.vipcxj.jasync.runtime.context;

import io.github.vipcxj.jasync.spec.JStack;

public class JStackImpl extends ArrayStack<Object> implements JStack {

    @Override
    protected Object[] createArray(int size) {
        return new Object[size];
    }
}
