package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

public class AsyncContext {
    private MethodContext methodContext;
    private Frame<? extends BasicValue> frame;
}
