package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.model.Frame;
import io.github.vipcxj.jasync.core.javac.translate.TransExpressionContext;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;

public class TransVarDeclContext extends AbstractTransStatementContext<JCTree.JCVariableDecl> {

    private Frame.DeclInfo declInfo;
    private TransExpressionContext<?> initContext;
    private boolean asyncParam;

    public TransVarDeclContext(AnalyzerContext analyzerContext, JCTree.JCVariableDecl tree) {
        super(analyzerContext, tree);
        this.asyncParam = false;
    }

    public boolean isAsyncParam() {
        return asyncParam;
    }

    public void setAsyncParam(boolean asyncParam) {
        this.asyncParam = asyncParam;
    }

    public Frame.DeclInfo getDeclInfo() {
        return declInfo;
    }

    public void setDeclInfo(Frame.DeclInfo declInfo) {
        this.declInfo = declInfo;
    }

    @Override
    protected void addNormalChildContext(TranslateContext<?> child) {
        if (tree.init == child.getTree()) {
            if (tree.init != null) {
                childContextMustBeExpression(child);
                initContext = (TransExpressionContext<?>) child;
            } else {
                initContext = null;
            }
        }
    }

    @Override
    protected JCTree awaitContainer() {
        return tree;
    }

    @Override
    public TransVarDeclContext enter() {
        super.enter();
        return this;
    }

    @Override
    public void complete() {
        analyzerContext.addLocal(this);
        super.complete();
    }

    @Override
    protected JCTree buildTreeWithoutThen(boolean replaceSelf) {
        if (tree.init != null) {
            tree.init = (JCTree.JCExpression) initContext.buildTree(false);
        }
        return tree;
    }
}
