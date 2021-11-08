package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;

public class TransLambdaContext extends AbstractTransFrameHolderExpressionContext<JCTree.JCLambda> {

    public TransLambdaContext(AnalyzerContext analyzerContext, JCTree.JCLambda tree) {
        super(analyzerContext, tree);
    }

    @Override
    public boolean isAwaitGap() {
        return true;
    }

    @Override
    protected void addNormalChildContext(TranslateContext<?> child) {
        if (tree.body != null && tree.body instanceof JCTree.JCBlock && tree.body == child.getTree()) {
            TransBlockContext blockContext = (TransBlockContext) child;
            blockContext.setProxyFrame(true);
            blockContext.setNude(true);
            blockContext.setDirect(true);
        }
    }
}
