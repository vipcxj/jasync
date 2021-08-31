package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;

public class TransContinueContext extends AbstractTransJumpLikeContext<JCTree.JCContinue> {

    public TransContinueContext(AnalyzerContext analyzerContext, JCTree.JCContinue tree) {
        super(analyzerContext, tree);
    }

    @Override
    protected JCTree.JCExpression makeExpr() {
        IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
        return jasyncContext.getJAsyncSymbols().makeDoContinue(tree);
    }
}
