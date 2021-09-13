package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.JavacUtils;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.translate.TransStatementContext;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;

public class TransLabeledContext extends AbstractTransStatementContext<JCTree.JCLabeledStatement> {

    private TranslateContext<?> innerContext;

    public TransLabeledContext(AnalyzerContext analyzerContext, JCTree.JCLabeledStatement tree) {
        super(analyzerContext, tree);
    }

    @Override
    public TransLabeledContext enter() {
        super.enter();
        return this;
    }

    @Override
    public void onChildEnter(TranslateContext<?> child) {
        super.onChildEnter(child);
        if (supportBreak(child)) {
            return;
        }
        new TransWrapBlockContext(analyzerContext).enter(false);
    }

    @Override
    protected void addNormalChildContext(TranslateContext<?> child) {
        if (innerContext == null) {
            checkContextTree(child, tree.body);
            this.innerContext = child;
            if (child instanceof TransAwaitContext) {
                ((TransStatementContext<?>) ((TransAwaitContext) child).getTargetContext()).setLabel(tree.label);
            } else {
                ((TransStatementContext<?>) this.innerContext).setLabel(tree.label);
            }
        } else {
            throwIfFull();
        }
    }

    private boolean supportContinue(TranslateContext<?> context) {
        if (context instanceof TransAwaitContext) {
            context = ((TransAwaitContext) context).getTargetContext();
        }
        return context instanceof TransWhileContext
                || context instanceof TransForContext
                || context instanceof TransDoWhileContext
                || context instanceof TransForeachContext;
    }

    private boolean supportBreak(TranslateContext<?> context) {
        if (context instanceof TransAwaitContext) {
            context = ((TransAwaitContext) context).getTargetContext();
        }
        return supportContinue(context)
                || context instanceof TransSwitchContext
                || context instanceof TransBlockContext;
    }

    @Override
    protected JCTree buildTreeWithoutThen(boolean replaceSelf) {
        if (hasAwait()) {
            JCTree outTree = innerContext.buildTree(false);
            if (outTree instanceof JCTree.JCExpression) {
                outTree = JavacUtils.makeReturn(getContext().getJasyncContext(), (JCTree.JCExpression) outTree);
            }
            return outTree;
        } else {
            tree.body = (JCTree.JCStatement) innerContext.buildTree(false);
            return tree;
        }
    }
}
