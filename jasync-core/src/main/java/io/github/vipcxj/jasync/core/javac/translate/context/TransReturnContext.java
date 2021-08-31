package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;

public class TransReturnContext extends AbstractTransStatementContext<JCTree.JCReturn> {
    public TransReturnContext(AnalyzerContext analyzerContext, JCTree.JCReturn tree) {
        super(analyzerContext, tree);
    }

    private boolean inAwaitScope() {
        TranslateContext<?> ctx = getParent();
        while (ctx != null) {
            JCTree tree = ctx.getTree();
            if (tree instanceof JCTree.JCMethodDecl
                    || tree instanceof JCTree.JCLambda
                    || tree instanceof JCTree.JCClassDecl
            ) {
                break;
            }
            if (ctx.hasAwait() ) {
                return true;
            }
            ctx = ctx.getParent();
        }
        return false;
    }

    @Override
    public JCTree buildTree(boolean replaceSelf) {
        if (inAwaitScope()) {
            IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
            return JavacUtils.makeReturn(jasyncContext, jasyncContext.getJAsyncSymbols().makeDoReturn(tree));
        } else {
            return tree;
        }
    }
}
