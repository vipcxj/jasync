package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;

public class TransAssignOpContext extends TransAssignLikeContext<JCTree.JCAssignOp> {

    public TransAssignOpContext(AnalyzerContext analyzerContext, JCTree.JCAssignOp tree) {
        super(analyzerContext, tree);
    }

    @Override
    public JCTree.JCExpression getVariableTree() {
        return tree.getVariable();
    }

    @Override
    public void setVariableTree(JCTree.JCExpression tree) {
        this.tree.lhs = tree;
    }

    @Override
    protected JCTree.JCExpression getExpressionTree() {
        return tree.getExpression();
    }

    @Override
    protected void setExpressionTree(JCTree.JCExpression tree) {
        this.tree.rhs = tree;
    }
}
