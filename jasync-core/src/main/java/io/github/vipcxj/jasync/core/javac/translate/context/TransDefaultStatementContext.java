package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;

public class TransDefaultStatementContext<T extends JCTree.JCStatement> extends AbstractTransStatementContext<T> {

    public TransDefaultStatementContext(AnalyzerContext analyzerContext, T tree) {
        super(analyzerContext, tree);
        awaitContainer = tree;
    }
}
