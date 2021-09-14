package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;

public class TransCatchContext extends AbstractTransFrameHolderContext<JCTree.JCCatch> {

    private TransVarDeclContext paramContext;
    private TransBlockContext bodyContext;

    public TransCatchContext(AnalyzerContext analyzerContext, JCTree.JCCatch tree) {
        super(analyzerContext, tree);
    }

    public TransBlockContext getBodyContext() {
        return bodyContext;
    }

    @Override
    public TransCatchContext enter(boolean triggerCallback) {
        super.enter(triggerCallback);
        return this;
    }

    @Override
    public void setAwaitScope(boolean awaitScope) {
        super.setAwaitScope(awaitScope);
        if (bodyContext != null)
            bodyContext.setAwaitScope(awaitScope);
    }

    @Override
    protected void addNormalChildContext(TranslateContext<?> child) {
        if (paramContext == null) {
            paramContext = (TransVarDeclContext) child;
            paramContext.setAsyncParam(true);
        } else if (bodyContext == null) {
            childContextMustBeBlock(child);
            bodyContext = (TransBlockContext) child;
            bodyContext.setProxyFrame(true);
            bodyContext.setNude(true);
        } else {
            throwIfFull();
        }
    }
}
