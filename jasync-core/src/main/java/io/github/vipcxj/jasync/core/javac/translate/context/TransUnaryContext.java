package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeMaker;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.model.Frame;
import io.github.vipcxj.jasync.core.javac.translate.TransExpressionContext;
import io.github.vipcxj.jasync.core.javac.translate.TransWriteExprContext;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;
import io.github.vipcxj.jasync.core.javac.translate.VarTransHelper;

public class TransUnaryContext
        extends AbstractTransExpressionContext<JCTree.JCUnary> implements TransWriteExprContext<JCTree.JCUnary> {

    private TransExpressionContext<?> argContext;
    private Frame.CapturedInfo capturedInfo;
    private Frame.DeclInfo declInfo;

    public TransUnaryContext(AnalyzerContext analyzerContext, JCTree.JCUnary tree) {
        super(analyzerContext, tree);
    }

    private boolean isWriteExpr() {
        return tree.getTag().isIncOrDecUnaryOp();
    }

    @Override
    public JCTree.JCExpression getVariableTree() {
        return isWriteExpr() ? tree.arg : null;
    }

    @Override
    public Symbol getSymbol() {
        JCTree.JCExpression variableTree = getVariableTree();
        return variableTree != null ? TreeInfo.symbol(variableTree) : null;
    }

    @Override
    public void setCapturedInfo(Frame.CapturedInfo capturedInfo) {
        this.capturedInfo = capturedInfo;
    }

    @Override
    public void setDeclInfo(Frame.DeclInfo declInfo) {
        this.declInfo = declInfo;
    }

    @Override
    protected void addNormalChildContext(TranslateContext<?> child) {
        if (argContext == null) {
            childContextMustBeExpression(child);
            checkContextTree(child, tree.arg);
            argContext = (TransExpressionContext<?>) child;
        } else {
            throwIfFull();
        }
    }

    @Override
    public void complete() {
        if (isWriteExpr()) {
            analyzerContext.writeVar(this);
        }
        super.complete();
    }

    @Override
    public JCTree buildTreeWithoutThen(boolean replaceSelf) {
        if (capturedInfo != null || declInfo != null) {
            VarTransHelper helper = new VarTransHelper(capturedInfo, declInfo);
            helper.prepare();
            JCTree.JCExpression newTree;
            if (helper.isRef()) {
                newTree = helper.makeRefAssign(analyzerContext.getJasyncContext(), tree, null);
            } else {
                TreeMaker maker = treeMaker();
                int prePos = maker.pos;
                try {
                    maker.pos = tree.pos;
                    tree.arg = maker.Ident(helper.getUsedSymbol());
                    newTree =  tree;
                } finally {
                    maker.pos = prePos;
                }
            }
            if (replaceSelf) {
                replaceBy(newTree);
            }
            return newTree;
        } else {
            tree.arg = (JCTree.JCExpression) argContext.buildTree(false);
            return tree;
        }
    }
}
