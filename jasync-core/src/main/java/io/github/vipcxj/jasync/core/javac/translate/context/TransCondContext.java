package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.ListBuffer;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.context.JAsyncSymbols;
import io.github.vipcxj.jasync.core.javac.model.Frame;
import io.github.vipcxj.jasync.core.javac.translate.TransExpressionContext;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;

public class TransCondContext extends AbstractTransFrameHolderExpressionContext<JCTree.JCExpression> {
    private TransExpressionContext<?> expressionContext;

    public TransCondContext(AnalyzerContext analyzerContext, JCTree.JCExpression expression) {
        super(analyzerContext, expression);
    }

    public TransExpressionContext<?> getExpressionContext() {
        return expressionContext;
    }

    @Override
    public TransCondContext enter() {
        super.enter();
        return this;
    }

    @Override
    public void onChildExit(TranslateContext<?> child) {
        exit(false);
    }

    @Override
    protected void addNormalChildContext(TranslateContext<?> child) {
        if (expressionContext == null) {
            childContextMustBeExpression(child);
            checkContextTree(child, tree);
            expressionContext = (TransExpressionContext<?>) child;
        } else {
            throwIfFull();
        }
    }

    @Override
    protected JCTree buildTreeWithoutThen(boolean replaceSelf) {
        if (hasAwait()) {
            ListBuffer<JCTree.JCStatement> stats = new ListBuffer<>();
            IJAsyncInstanceContext jasyncContext = getContext().getJasyncContext();
            JAsyncSymbols symbols = jasyncContext.getJAsyncSymbols();
            TreeMaker maker = treeMaker();
            int prePos = maker.pos;
            try {
                for (Frame.CapturedInfo capturedInfo : getFrame().getCapturedVars().values()) {
                    if (capturedInfo.isNotReadOnly()) {
                        safeMaker();
                        stats = stats.append(capturedInfo.makeUsedDecl());
                    }
                }
                JCTree expr = expressionContext.buildTree(false);
                if (expressionContext instanceof TransAwaitContext) {
                    for (JCTree.JCVariableDecl decl : ((TransAwaitContext) expressionContext).getProxyDecls().toList()) {
                        stats = stats.append(decl);
                    }
                }
                if (!(expr instanceof JCTree.JCReturn)) {
                    expr = JavacUtils.makeReturn(jasyncContext, symbols.makeJust((JCTree.JCExpression) expr));
                }
                stats = stats.append((JCTree.JCStatement) expr);
                return JavacUtils.makeBlock(jasyncContext, stats.toList());
            } finally {
                maker.pos = prePos;
            }
        } else {
            return expressionContext.buildTree(false);
        }
    }
}
