package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;

public abstract class TransWhileLikeContext<T extends JCTree.JCStatement> extends AbstractTransStatementContext<T> {

    protected ChildState childState;
    protected TransBlockContext bodyContext;
    protected TransBlockContext condContext;

    public TransWhileLikeContext(AnalyzerContext analyzerContext, T tree) {
        super(analyzerContext, tree);
    }

    protected void addCond(TranslateContext<?> child) {
        childContextMustBeBlock(child);
        condContext = (TransBlockContext) child;
        condContext.setNude(true);
    }

    protected void addBody(TranslateContext<?> child) {
        childContextMustBeBlock(child);
        bodyContext = (TransBlockContext) child;
        bodyContext.setNude(true);
    }

    @Override
    public void exit(boolean triggerCallback) {
        if (hasAwait()) {
            if (bodyContext != null) {
                bodyContext.setAwaitScope(true);
            }
            if (condContext != null) {
                condContext.setAwaitScope(true);
            }
        } else {
            if (condContext != null) {
                condContext.setProxyFrame(true);
            }
        }
        super.exit(triggerCallback);
    }

    enum ChildState {
        COND, BODY, COMPLETE
    }

    protected boolean hasAwaitCond() {
        return condContext != null && condContext.innerHasAwait();
    }

    protected abstract JCTree.JCExpression getBuildMethod();
    protected abstract void setCondTree(JCTree.JCExpression condTree);
    protected abstract void setBodyTree(JCTree.JCStatement bodyTree);

    @Override
    protected JCTree buildTreeWithoutThen(boolean replaceSelf) {
        if (hasAwait()) {
            IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
            TransMethodContext methodContext = getEnclosingMethodContext();
            TreeMaker maker = jasyncContext.getTreeMaker();
            int prePos = maker.pos;
            try {
                maker.pos = tree.pos;
                return JavacUtils.makeReturn(jasyncContext, safeMaker().Apply(
                        List.nil(),
                        getBuildMethod(),
                        List.of(
                                methodContext.makeCondSupplier(condContext),
                                methodContext.makeVoidPromiseSupplier(bodyContext),
                                makeLabelArg()
                        )
                ));
            } finally {
                maker.pos = prePos;
            }
        } else {
            setCondTree(condContext != null ? (JCTree.JCExpression) condContext.getSingleChild().buildTree(false) : null);
            setBodyTree(bodyContext != null ? (JCTree.JCStatement) bodyContext.buildTree(false) : null);
            return tree;
        }
    }
}
