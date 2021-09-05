package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;

public class TransForContext extends AbstractTransStatementContext<JCTree.JCForLoop> {
    public TransForContext(AnalyzerContext analyzerContext, JCTree.JCForLoop tree) {
        super(analyzerContext, tree);
    }
}
