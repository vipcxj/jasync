package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;

public class TransWrapBlockContext extends TransBlockContext {

    private TranslateContext<?> wrappedContext;

    public TransWrapBlockContext(AnalyzerContext analyzerContext) {
        super(analyzerContext, null, true);
    }

    public TranslateContext<?> getWrappedContext() {
        return wrappedContext;
    }

    @Override
    public void onChildExit(TranslateContext<?> child) {
        exit(false);
    }

    @Override
    protected void addNormalChildContext(TranslateContext<?> child) {
        if (wrappedContext == null) {
            wrappedContext = child;
        } else {
            throw new IllegalStateException("Only one wrapped context is permitted.");
        }
        super.addNormalChildContext(child);
    }
}
