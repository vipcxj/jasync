package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;

public class TransLambdaContext extends AbstractTransFrameHolderExpressionContext<JCTree.JCLambda> {
    public TransLambdaContext(AnalyzerContext analyzerContext, JCTree.JCLambda tree) {
        super(analyzerContext, tree);
    }

    @Override
    public boolean isAwaitGap() {
        return true;
    }
}
