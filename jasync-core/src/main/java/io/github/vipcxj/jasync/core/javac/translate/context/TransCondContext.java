package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.ListBuffer;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.model.Frame;
import io.github.vipcxj.jasync.core.javac.translate.TransDecorator;
import io.github.vipcxj.jasync.core.javac.translate.TransExpressionContext;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;

public class TransCondContext extends AbstractTransFrameHolderExpressionContext<JCTree.JCExpression> implements TransDecorator {
    private TransExpressionContext<?> exprContext;

    public TransCondContext(AnalyzerContext analyzerContext, JCTree.JCExpression expression) {
        super(analyzerContext, expression);
    }

    public boolean hasAwaitExpr() {
        return exprContext.hasAwait();
    }

    @Override
    public TransCondContext enter() {
        super.enter();
        return this;
    }

    @Override
    public void exit(boolean triggerCallback) {
        if (exprContext != null && exprContext.inThen()) {
            exprContext.endThen();
        }
        super.exit(triggerCallback);
    }

    @Override
    public JCTree decorate(TranslateContext<?> ctx, JCTree tree) {
        IJAsyncInstanceContext jasyncContext = getContext().getJasyncContext();
        return JavacUtils.makeReturn(
                jasyncContext,
                hasAwaitExpr()
                        ? jasyncContext.getJAsyncSymbols().makeJust((JCTree.JCExpression) tree)
                        : (JCTree.JCExpression) tree
        );
    }

    @Override
    protected void addNormalChildContext(TranslateContext<?> child) {
        if (exprContext == null) {
            childContextMustBeExpression(child);
            checkContextTree(child, tree);
            exprContext = (TransExpressionContext<?>) child;
        } else {
            throwIfFull();
        }
    }

    @Override
    protected JCTree buildTreeWithoutThen(boolean replaceSelf) {
        if (hasAwait()) {
            ListBuffer<JCTree.JCStatement> stats = new ListBuffer<>();
            IJAsyncInstanceContext jasyncContext = getContext().getJasyncContext();
            TreeMaker maker = treeMaker();
            int prePos = maker.pos;
            try {
                for (Frame.CapturedInfo capturedInfo : getFrame().getCapturedVars().values()) {
                    if (capturedInfo.isNotReadOnly()) {
                        safeMaker();
                        stats = stats.append(capturedInfo.makeUsedDecl());
                    }
                }
                if (isDebug()) {
                    for (Frame.CapturedInfo capturedInfo : getFrame().getDebugCapturedVars().values()) {
                        if (capturedInfo.isNotReadOnly()) {
                            stats = stats.append(capturedInfo.makeUsedDecl());
                        }
                    }
                }
                JCTree expr = exprContext.buildTree(false);
                if (exprContext instanceof TransAwaitContext) {
                    for (JCTree.JCVariableDecl decl : ((TransAwaitContext) exprContext).getProxyDecls().toList()) {
                        stats = stats.append(decl);
                    }
                }
                stats = stats.append((JCTree.JCStatement) expr);
                return JavacUtils.makeBlock(jasyncContext, stats.toList());
            } finally {
                maker.pos = prePos;
            }
        } else {
            return exprContext.buildTree(false);
        }
    }
}
