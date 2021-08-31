package io.github.vipcxj.jasync.core.javac.visitor;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;

public class AwaitScanner extends ShallowTreeBooleanScanner {

    private final IJAsyncInstanceContext context;
    private boolean result;

    public AwaitScanner(IJAsyncInstanceContext context) {
        this.context = context;
        this.result = false;
    }

    @Override
    public Boolean visitMethodInvocation(MethodInvocationTree methodInvocationTree, Void aVoid) {
        if (result) return true;
        result = JavacUtils.isAwaitTree(context, (JCTree) methodInvocationTree);
        return result;
    }

    public static boolean checkTree(IJAsyncInstanceContext context, JCTree tree) {
        AwaitScanner scanner = new AwaitScanner(context);
        return Boolean.TRUE.equals(tree.accept(scanner, null));
    }
}
