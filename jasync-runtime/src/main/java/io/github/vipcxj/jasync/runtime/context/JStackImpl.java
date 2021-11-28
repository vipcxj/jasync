package io.github.vipcxj.jasync.runtime.context;

import io.github.vipcxj.jasync.spec.JStack;

import java.util.Deque;

public class JStackImpl implements JStack {
    private final Deque<Object> stack;

    public JStackImpl(Deque<Object> stack) {
        this.stack = stack;
    }

    @Override
    public Object pop() {
        return stack.pop();
    }
}
