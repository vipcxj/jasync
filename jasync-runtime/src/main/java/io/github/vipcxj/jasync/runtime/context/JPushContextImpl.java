package io.github.vipcxj.jasync.runtime.context;

import io.github.vipcxj.jasync.spec.JContext;
import io.github.vipcxj.jasync.spec.JPromise2;
import io.github.vipcxj.jasync.spec.JPushContext;

public class JPushContextImpl implements JPushContext {

    private final JStackImpl stack;

    public JPushContextImpl() {
        this.stack = new JStackImpl();
    }

    @Override
    public JPushContext push(Object v) {
        this.stack.push(v);
        return this;
    }

    @Override
    public JPromise2<JContext> complete() {
        return JContext.current().thenImmediate(ctx -> ctx.pushStack(stack));
    }
}
