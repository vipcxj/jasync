package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;

public abstract class AbstractTransJumpLikeContext<T extends JCTree.JCStatement> extends AbstractTransStatementContext<T> {
    public AbstractTransJumpLikeContext(AnalyzerContext analyzerContext, T tree) {
        super(analyzerContext, tree);
    }

    private boolean inAwaitScope() {
        TranslateContext<?> ctx = getParent();
        while (ctx != null) {
            JCTree tree = ctx.getTree();
            if (tree instanceof JCTree.JCWhileLoop
                    || tree instanceof JCTree.JCDoWhileLoop
                    || tree instanceof JCTree.JCForLoop
                    || tree instanceof JCTree.JCEnhancedForLoop
                    || tree instanceof JCTree.JCLabeledStatement
                    || tree instanceof JCTree.JCSwitch
            ) {
                return ctx.hasAwait();
            }
            ctx = ctx.getParent();
        }
        return false;
    }

    @Override
    public JCTree buildTree(boolean replaceSelf) {
        if (inAwaitScope()) {
            IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
            return JavacUtils.makeExprStat(jasyncContext, makeExpr());
        } else {
            return tree;
        }
    }

    protected abstract JCTree.JCExpression makeExpr();
}

