package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;

public class TransDefaultExpressionContext<T extends JCTree.JCExpression> extends AbstractTransExpressionContext<T> {

    public TransDefaultExpressionContext(AnalyzerContext analyzerContext, T tree) {
        super(analyzerContext, tree);
    }
}
