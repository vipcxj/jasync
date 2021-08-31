package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.translate.TransStatementContext;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;

import java.util.ArrayList;
import java.util.List;

public class TransCaseContext extends AbstractTranslateContext<JCTree.JCCase> {

    private TranslateContext<?> patContext;
    private List<TransStatementContext<?>> statementContexts;

    public TransCaseContext(AnalyzerContext analyzerContext, JCTree.JCCase tree) {
        super(analyzerContext, tree);
        this.statementContexts = new ArrayList<>();
    }

    public TranslateContext<?> getPatContext() {
        return patContext;
    }

    public TransBlockContext getSingleBodyContext() {
        if (statementContexts.size() == 1) {
            TransStatementContext<?> statementContext = statementContexts.get(0);
            if (statementContext instanceof TransBlockContext) {
                return (TransBlockContext) statementContext;
            }
        }
        throw new IllegalArgumentException("Only can be called when single block statement in case");
    }

    @Override
    public TransCaseContext enter(boolean triggerCallback) {
        super.enter(triggerCallback);
        return this;
    }

    private int statsNum() {
        return tree.stats != null ? tree.stats.size() : 0;
    }

    @Override
    protected void addNormalChildContext(TranslateContext<?> child) {
        if (patContext == null) {
            patContext = child;
        } else if (statementContexts.size() < statsNum()) {
            JCTree.JCStatement statement = tree.stats.get(statementContexts.size());
            childContextMustBeStatement(child);
            checkContextTree(child, statement);
            statementContexts.add((TransStatementContext<?>) child);
            if (child instanceof TransBlockContext) {
                ((TransBlockContext) child).setNude(true);
            }
        } else {
            throwIfFull();
        }
    }
}
