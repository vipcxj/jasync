package io.github.vipcxj.jasync.runtime.context;

import io.github.vipcxj.jasync.spec.*;

import java.util.ArrayDeque;
import java.util.Deque;

public class JPushContextImpl implements JPushContext {

    private final Deque<Object> stack;

    public JPushContextImpl() {
        this.stack = new ArrayDeque<>();
    }

    @Override
    public JPushContext push(Object v) {
        this.stack.push(v);
        return this;
    }

    @Override
    public JPromise2<JContext> complete() {
        JStack readOnlyStack = new JStackImpl(stack);
        return JContext.current().thenImmediate(ctx -> ctx.pushStack(readOnlyStack));
    }
}
