package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.context.JAsyncSymbols;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;

public class TransDoWhileContext extends TransWhileLikeContext<JCTree.JCDoWhileLoop> {

    public TransDoWhileContext(AnalyzerContext analyzerContext, JCTree.JCDoWhileLoop tree) {
        super(analyzerContext, tree);
        this.childState = ChildState.BODY;
    }

    @Override
    public TransDoWhileContext enter() {
        super.enter();
        return this;
    }

    @Override
    protected void addNormalChildContext(TranslateContext<?> child) {
        if (bodyContext == null) {
            addBody(child, tree.body);
            childState = ChildState.COND;
        } else if (condContext == null) {
            addCond(child, tree.cond);
            childState = ChildState.COMPLETE;
        } else  {
            throwIfFull();
        }
    }

    @Override
    protected JCTree.JCExpression getBuildMethod() {
        JAsyncSymbols symbols = analyzerContext.getJasyncContext().getJAsyncSymbols();
        return condContext.hasAwait() ? symbols.makeJAsyncDoDoPromiseWhile() : symbols.makeJAsyncDoDoWhile();
    }

    @Override
    protected JCTree buildTreeWithoutThen(boolean replaceSelf) {
        if (hasAwait()) {
            return super.buildTreeWithoutThen(replaceSelf);
        } else {
            tree.cond = (JCTree.JCExpression) condContext.buildTree(false);
            tree.body = (JCTree.JCStatement) bodyContext.buildTree(false);
            return tree;
        }
    }
}
