package io.github.vipcxj.jasync.ng.runtime.context;

import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JPushContext;

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
    public JPromise<JContext> complete() {
        return JPromise.updateContext(ctx -> ctx.pushStack(stack));
    }
}
