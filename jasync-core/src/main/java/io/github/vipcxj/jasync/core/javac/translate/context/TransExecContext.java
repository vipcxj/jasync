package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeMaker;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;

public class TransExecContext extends TransDefaultStatementContext<JCTree.JCExpressionStatement> {

    private TranslateContext<?> exprContext;

    public TransExecContext(AnalyzerContext analyzerContext, JCTree.JCExpressionStatement tree) {
        super(analyzerContext, tree);
    }

    @Override
    protected void addNormalChildContext(TranslateContext<?> child) {
        if (exprContext == null) {
            exprContext = child;
        } else {
            throwIfFull();
        }
    }

    @Override
    protected JCTree buildTreeWithoutThen(boolean replaceSelf) {
        IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
        TreeMaker maker = jasyncContext.getTreeMaker();
        JCTree.JCExpression expr = (JCTree.JCExpression) exprContext.buildTree(false);
        expr = TreeInfo.skipParens(expr);
        if (expr instanceof JCTree.JCIdent) {
            return maker.Skip();
        } else {
            tree.expr = expr;
            return tree;
        }
    }
}
