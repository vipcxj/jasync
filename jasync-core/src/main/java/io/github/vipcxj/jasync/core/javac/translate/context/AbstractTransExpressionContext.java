package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.translate.TransExpressionContext;

public class AbstractTransExpressionContext<T extends JCTree.JCExpression> extends AbstractTranslateContext<T> implements TransExpressionContext<T> {

    public AbstractTransExpressionContext(AnalyzerContext analyzerContext, T tree) {
        super(analyzerContext, tree);
    }
}
