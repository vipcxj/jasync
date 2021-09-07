package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.context.JAsyncSymbols;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;

public class TransDoWhileContext extends TransWhileLikeContext<JCTree.JCDoWhileLoop> {

    public TransDoWhileContext(AnalyzerContext analyzerContext, JCTree.JCDoWhileLoop tree) {
        super(analyzerContext, tree);
        this.childState = tree.body != null
                ? ChildState.BODY
                : tree.cond != null
                ? ChildState.COND
                : ChildState.COMPLETE;
    }

    @Override
    public TransDoWhileContext enter() {
        super.enter();
        return this;
    }

    @Override
    protected void addNormalChildContext(TranslateContext<?> child) {
        if (childState == ChildState.BODY) {
            addBody(child);
            childState = tree.cond != null
                    ? ChildState.COND
                    : ChildState.COMPLETE;
        } else if (childState == ChildState.COND) {
            addCond(child);
            childState = ChildState.COMPLETE;
        } else  {
            throwIfFull();
        }
    }

    @Override
    protected JCTree.JCExpression getBuildMethod() {
        JAsyncSymbols symbols = analyzerContext.getJasyncContext().getJAsyncSymbols();
        return hasAwaitCond() ? symbols.makeJAsyncDoDoPromiseWhile() : symbols.makeJAsyncDoDoWhile();
    }

    @Override
    protected void setCondTree(JCTree.JCExpression condTree) {
        tree.cond = condTree;
    }

    @Override
    protected void setBodyTree(JCTree.JCStatement bodyTree) {
        tree.body = bodyTree;
    }
}
