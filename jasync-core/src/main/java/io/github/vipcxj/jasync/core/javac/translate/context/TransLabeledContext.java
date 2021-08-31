package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.translate.TransStatementContext;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;

public class TransLabeledContext extends AbstractTransStatementContext<JCTree.JCLabeledStatement> {

    private TransStatementContext<?> statementContext;

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
        if (child.hasAwait()) {
            if (child instanceof TransBlockContext || isLoop(child)) {
                return;
            }
            new TransWrapBlockContext(analyzerContext).enter(false);
        }
    }

    @Override
    protected void addNormalChildContext(TranslateContext<?> child) {
        if (statementContext == null) {
            mustBeLoopOrBlockContext(child);
            checkContextTree(child, tree.body);
            this.statementContext = (TransStatementContext<?>) child;
            this.statementContext.setLabel(tree.label);
        } else {
            throwIfFull();
        }
    }

    private boolean isLoop(TranslateContext<?> context) {
        return context instanceof TransWhileContext
                || context instanceof TransDoWhileContext
                || context instanceof TransForeachContext;
    }

    private void mustBeLoopOrBlockContext(TranslateContext<?> child) {
        if (child instanceof TransBlockContext || isLoop(child)) {
            return;
        }
        throw new IllegalArgumentException("The child must be loop or block context.");
    }
}
