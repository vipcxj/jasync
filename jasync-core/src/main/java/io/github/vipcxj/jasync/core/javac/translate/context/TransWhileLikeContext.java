package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import io.github.vipcxj.jasync.core.javac.Constants;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;

public abstract class TransWhileLikeContext<T extends JCTree.JCStatement> extends AbstractTransStatementContext<T> {

    protected ChildState childState;
    protected TransBlockContext bodyContext;
    protected TransCondContext condContext;

    public TransWhileLikeContext(AnalyzerContext analyzerContext, T tree) {
        super(analyzerContext, tree);
    }

    protected void addCond(TranslateContext<?> child, JCTree.JCExpression cond) {
        assert child instanceof TransCondContext;
        checkContextTree(child, cond);
        condContext = (TransCondContext) child;
    }

    protected void addBody(TranslateContext<?> child, JCTree.JCStatement body) {
        childContextMustBeBlock(child);
        checkContextTree(child, body);
        bodyContext = (TransBlockContext) child;
        bodyContext.setNude(true);
    }

    @Override
    public void onChildEnter(TranslateContext<?> child) {
        super.onChildEnter(child);
        if (childState == ChildState.COND) {
            childContextMustBeExpression(child);
            child.setAwaitContainer(child.getTree());
            TransCondContext condContext = new TransCondContext(analyzerContext, (JCTree.JCExpression) child.getTree());
            child.addDecorator(condContext);
            child.addPostExitTrigger(ctx -> condContext.exit(false));
            condContext.enter(false);
        } else if (childState == ChildState.BODY) {
            if (child instanceof TransBlockContext) {
                return;
            }
            new TransWrapBlockContext(analyzerContext).enter(false);
        }
    }

    @Override
    public void exit(boolean triggerCallback) {
        if (hasAwait()) {
            bodyContext.setHasAwait(true);
            condContext.setHasAwait(true);
        }
        super.exit(triggerCallback);
    }

    enum ChildState {
        COND, BODY, COMPLETE
    }

    protected abstract JCTree.JCExpression getBuildMethod();

    @Override
    protected JCTree buildTreeWithoutThen(boolean replaceSelf) {
        if (hasAwait()) {
            IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
            TransMethodContext methodContext = getEnclosingMethodContext();
            JCTree.JCMethodDecl condMethod = methodContext.addCondSupplier(condContext);
            JCTree.JCMethodDecl bodyMethod = methodContext.addVoidPromiseSupplier(bodyContext);
            TreeMaker maker = jasyncContext.getTreeMaker();
            int prePos = maker.pos;
            try {
                maker.pos = tree.pos;
                String methodType = condContext.hasAwaitExpr() ? Constants.INDY_MAKE_PROMISE_SUPPLIER : Constants.INDY_MAKE_BOOLEAN_SUPPLIER;
                return JavacUtils.makeReturn(jasyncContext, safeMaker().Apply(
                        List.nil(),
                        getBuildMethod(),
                        List.of(
                                methodContext.makeFunctional(condContext.getFrame(), methodType, condMethod),
                                methodContext.makeFunctional(bodyContext.getFrame(), Constants.INDY_MAKE_VOID_PROMISE_SUPPLIER, bodyMethod),
                                makeLabelArg()
                        )
                ));
            } finally {
                maker.pos = prePos;
            }
        } else {
            return null;
        }
    }
}
