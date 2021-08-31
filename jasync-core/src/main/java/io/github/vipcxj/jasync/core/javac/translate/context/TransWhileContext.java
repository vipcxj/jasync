package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.context.JAsyncSymbols;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;

public class TransWhileContext extends TransWhileLikeContext<JCTree.JCWhileLoop> {

    public TransWhileContext(AnalyzerContext analyzerContext, JCTree.JCWhileLoop tree) {
        super(analyzerContext, tree);
        this.childState = ChildState.COND;
    }

    @Override
    public TransWhileContext enter() {
        super.enter();
        return this;
    }

    @Override
    protected void addNormalChildContext(TranslateContext<?> child) {
        if (condContext == null) {
            addCond(child, tree.cond);
            childState = ChildState.BODY;
        } else if (bodyContext == null) {
            addBody(child, tree.body);
            childState = ChildState.COMPLETE;
        } else {
            throwIfFull();
        }
    }

    @Override
    protected JCTree.JCExpression getBuildMethod() {
        JAsyncSymbols symbols = analyzerContext.getJasyncContext().getJAsyncSymbols();
        return condContext.hasAwait() ? symbols.makeJAsyncDoPromiseWhile() : symbols.makeJAsyncDoWhile();
    }

    @Override
    protected JCTree buildTreeWithoutThen(boolean replaceSelf) {
        if (hasAwait()) {
            return super.buildTreeWithoutThen(replaceSelf);
        } else {
            tree.cond = (JCTree.JCExpression) condContext.buildTree(false);
            tree.body = (JCTree.JCStatement) condContext.buildTree(false);
            return tree;
        }
    }
}
