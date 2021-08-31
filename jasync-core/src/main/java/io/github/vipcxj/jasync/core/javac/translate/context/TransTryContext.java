package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;
import io.github.vipcxj.jasync.core.javac.Constants;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;

public class TransTryContext extends AbstractTransStatementContext<JCTree.JCTry> {

    private ListBuffer<TranslateContext<?>> resourceContexts;
    private TransBlockContext bodyContext;
    private ListBuffer<TransCatchContext> catchContexts;
    private TransBlockContext finallyContext;

    public TransTryContext(AnalyzerContext analyzerContext, JCTree.JCTry tree) {
        super(analyzerContext, tree);
        this.resourceContexts = new ListBuffer<>();
        this.catchContexts = new ListBuffer<>();
    }

    @Override
    public TransTryContext enter() {
        super.enter();
        return this;
    }

    @Override
    public void exit() {
        if (hasAwait()) {
            bodyContext.setHasAwait(true);
            for (TransCatchContext catchContext : catchContexts) {
                catchContext.setHasAwait(true);
            }
            if (finallyContext != null) {
                finallyContext.setHasAwait(true);
            }
        }
        super.exit();
    }

    private int resourceNum() {
        return tree != null && tree.resources != null ? tree.resources.size() : 0;
    }
    private int catcherNum() {
        return tree != null && tree.catchers != null ? tree.catchers.size() : 0;
    }

    @Override
    protected void addNormalChildContext(TranslateContext<?> child) {
        if (resourceContexts.size() < resourceNum()) {
            JCTree rsTree = tree.resources.get(resourceContexts.size());
            checkContextTree(child, rsTree);
            resourceContexts = resourceContexts.append(child);
        } else if (bodyContext == null) {
            childContextMustBeBlock(child);
            checkContextTree(child, tree.body);
            bodyContext = (TransBlockContext) child;
            bodyContext.setNude(true);
        } else if (catchContexts.size() < catcherNum()) {
            JCTree.JCCatch jcCatch = tree.catchers.get(catchContexts.size());
            checkContextTree(child, jcCatch);
            catchContexts = catchContexts.append((TransCatchContext) child);
        } else if (finallyContext == null) {
            childContextMustBeBlock(child);
            checkContextTree(child, tree.finalizer);
            finallyContext = (TransBlockContext) child;
            finallyContext.setNude(true);
        } else {
            throwIfFull();
        }
    }

    private JCTree.JCExpression buildCatch(JCTree.JCExpression outExpr, JCTree.JCExpression exType, TransCatchContext catchContext) {
        IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
        TreeMaker maker = jasyncContext.getTreeMaker();
        Names names = jasyncContext.getNames();
        TransMethodContext methodContext = getEnclosingMethodContext();
        JCTree.JCMethodDecl methodDecl = methodContext.addThrowableConsumer(catchContext);
        return maker.Apply(
                List.nil(),
                maker.Select(outExpr, names.fromString(Constants.DO_CATCH)),
                List.of(
                        maker.Select(exType, names._class),
                        methodContext.makeFunctional(catchContext.getFrame(), Constants.INDY_MAKE_THROWABLE_CONSUMER, methodDecl)
                )
        );
    }

    @Override
    protected JCTree buildTreeWithoutThen(boolean replaceSelf) {
        if (hasAwait()) {
            // After normalize, try with resources are all transformed to try without resources.
            if (!resourceContexts.isEmpty()) {
                throw new IllegalStateException("This is impossible.");
            }
            // TransBlockContext build JAsync.deferVoid(...) by default.
            JCTree.JCExpression bodyExpr = (JCTree.JCExpression) bodyContext.buildTree(false);
            for (TransCatchContext catchContext : catchContexts) {
                JCTree.JCVariableDecl exParam = catchContext.tree.param;
                JCTree.JCExpression exType = exParam.vartype;
                if (exType.getKind() == Tree.Kind.UNION_TYPE) {
                    JCTree.JCTypeUnion typeUnion = (JCTree.JCTypeUnion) exType;
                    for (JCTree.JCExpression alternative : typeUnion.alternatives) {
                        bodyExpr = buildCatch(bodyExpr, alternative, catchContext);
                    }
                } else {
                    bodyExpr = buildCatch(bodyExpr, exType, catchContext);
                }
            }
            IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
            TreeMaker maker = jasyncContext.getTreeMaker();
            if (finallyContext != null) {
                Names names = jasyncContext.getNames();
                TransMethodContext methodContext = getEnclosingMethodContext();
                JCTree.JCMethodDecl methodDecl = methodContext.addVoidPromiseSupplier(finallyContext);
                bodyExpr = maker.Apply(
                        List.nil(),
                        maker.Select(bodyExpr, names.fromString(Constants.DO_FINALLY)),
                        List.of(
                                methodContext.makeFunctional(finallyContext.getFrame(), Constants.INDY_MAKE_VOID_PROMISE_SUPPLIER, methodDecl)
                        )
                );
            }
            return maker.Return(bodyExpr);
        } else {
            tree.resources = JavacUtils.mapList(resourceContexts.toList(), ctx -> ctx.buildTree(false));
            tree.body = (JCTree.JCBlock) bodyContext.buildTree(false);
            tree.catchers = JavacUtils.mapList(catchContexts.toList(), ctx -> (JCTree.JCCatch) ctx.buildTree(false));
            tree.finalizer = (JCTree.JCBlock) finallyContext.buildTree(false);
            return tree;
        }
    }
}
