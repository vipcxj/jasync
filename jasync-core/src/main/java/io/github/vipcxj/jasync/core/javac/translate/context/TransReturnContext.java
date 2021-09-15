package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.context.JAsyncSymbols;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;

public class TransReturnContext extends AbstractTransStatementContext<JCTree.JCReturn> {

    private TranslateContext<?> exprContext;

    public TransReturnContext(AnalyzerContext analyzerContext, JCTree.JCReturn tree) {
        super(analyzerContext, tree);
        awaitContainer = tree;
    }

    @Override
    protected void addNormalChildContext(TranslateContext<?> child) {
        checkContextTree(child, tree.expr);
        exprContext = child;
    }

    private static boolean isNull(JCTree tree) {
        if (tree instanceof JCTree.JCLiteral) {
            JCTree.JCLiteral literal = (JCTree.JCLiteral) tree;
            return literal.value == null;
        }
        return false;
    }

    private boolean inAwaitScope() {
        TranslateContext<?> ctx = getParent();
        while (ctx != null) {
            JCTree tree = ctx.getTree();
            if (tree instanceof JCTree.JCMethodDecl
                    || tree instanceof JCTree.JCLambda
                    || tree instanceof JCTree.JCClassDecl
            ) {
                break;
            }
            if (ctx instanceof TransBlockContext && ((TransBlockContext) ctx).isDirect()) {
                break;
            }
            if (ctx.hasAwait()) {
                return true;
            }
            ctx = ctx.getParent();
        }
        return false;
    }

    @Override
    public JCTree buildTreeWithoutThen(boolean replaceSelf) {
        JCTree.JCExpression expr = exprContext != null ? (JCTree.JCExpression) exprContext.buildTree(false) : null;
        IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
        JAsyncSymbols symbols = jasyncContext.getJAsyncSymbols();
        if (inAwaitScope()) {
            tree.expr = symbols.makeDoReturn(tree, expr);
        } else if (isNull(getTree().expr)) {
            tree.expr = symbols.makeJust();
        } else {
            tree.expr = expr;
        }
        return tree;
    }
}
