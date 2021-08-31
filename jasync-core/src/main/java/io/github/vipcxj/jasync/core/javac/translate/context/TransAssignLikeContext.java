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

public abstract class TransAssignLikeContext<T extends JCTree.JCExpression>
        extends AbstractTransExpressionContext<T>  implements TransWriteExprContext<T> {

    protected TransExpressionContext<?> variableContext;
    protected TransExpressionContext<?> expressionContext;
    protected Frame.CapturedInfo capturedInfo;
    protected Frame.DeclInfo declInfo;

    public TransAssignLikeContext(AnalyzerContext analyzerContext, T tree) {
        super(analyzerContext, tree);
    }

    protected abstract void setVariableTree(JCTree.JCExpression tree);
    protected abstract JCTree.JCExpression getExpressionTree();
    protected abstract void setExpressionTree(JCTree.JCExpression tree);

    @Override
    public Symbol getSymbol() {
        JCTree.JCExpression variableTree = getVariableTree();
        return TreeInfo.symbol(variableTree);
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
        if (variableContext == null) {
            childContextMustBeExpression(child);
            checkContextTree(child, getVariableTree());
            variableContext = (TransExpressionContext<?>) child;
        } else if (expressionContext == null) {
            childContextMustBeExpression(child);
            checkContextTree(child, getExpressionTree());
            expressionContext = (TransExpressionContext<?>) child;
        } else {
            throwIfFull();
        }
    }

    @Override
    public void complete() {
        super.complete(false);
        analyzerContext.writeVar(this);
        if (thenContext != null) {
            thenContext.complete();
        }
    }

    @Override
    public JCTree buildTree(boolean replaceSelf) {
        JCTree.JCExpression expression = (JCTree.JCExpression) expressionContext.buildTree(false);
        setExpressionTree(expression);
        if (capturedInfo != null || declInfo != null) {
            VarTransHelper helper = new VarTransHelper(capturedInfo, declInfo);
            helper.prepare();
            JCTree.JCExpression newTree;
            if (helper.isRef()) {
                newTree = helper.makeRefAssign(analyzerContext.getJasyncContext(), tree, expression);
            } else {
                TreeMaker maker = treeMaker();
                int prePos = maker.pos;
                try {
                    maker.pos = tree.pos;
                    setVariableTree(maker.Ident(helper.getUsedSymbol()));
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
            return tree;
        }
    }
}
