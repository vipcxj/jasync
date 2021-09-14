package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.context.JAsyncSymbols;
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
    public void exit(boolean triggerCallback) {
        if (hasAwait()) {
            bodyContext.setAwaitScope(true);
            for (TransCatchContext catchContext : catchContexts) {
                catchContext.setAwaitScope(true);
            }
            if (finallyContext != null) {
                finallyContext.setAwaitScope(true);
            }
        }
        super.exit(triggerCallback);
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
            // bodyContext.setNude(true);
        } else if (catchContexts.size() < catcherNum()) {
            JCTree.JCCatch jcCatch = tree.catchers.get(catchContexts.size());
            checkContextTree(child, jcCatch);
            catchContexts = catchContexts.append((TransCatchContext) child);
        } else if (finallyContext == null) {
            if (!(child instanceof TransNullContext)) {
                childContextMustBeBlock(child);
                checkContextTree(child, tree.finalizer);
                finallyContext = (TransBlockContext) child;
                finallyContext.setNude(true);
            }
        } else {
            throwIfFull();
        }
    }

    private JCTree.JCExpression buildCatch(JCTree.JCExpression exType, TransCatchContext catchContext) {
        IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
        JAsyncSymbols symbols = jasyncContext.getJAsyncSymbols();
        TransMethodContext methodContext = getEnclosingMethodContext();
        return safeMaker().Apply(
                List.nil(),
                symbols.makeCatcherOf(),
                List.of(
                        safeMaker().ClassLiteral(exType.type),
                        methodContext.makePromiseFunction(catchContext.getBodyContext(), JavacUtils.getBoxedVoidType(jasyncContext))
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
            JCTree.JCReturn bodyTree = (JCTree.JCReturn) bodyContext.buildTree(false);
            IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
            JAsyncSymbols symbols = jasyncContext.getJAsyncSymbols();
            if (catchContexts != null && !catchContexts.isEmpty()) {
                ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
                for (TransCatchContext catchContext : catchContexts) {
                    JCTree.JCVariableDecl exParam = catchContext.tree.param;
                    JCTree.JCExpression exType = exParam.vartype;
                    if (exType.getKind() == Tree.Kind.UNION_TYPE) {
                        JCTree.JCTypeUnion typeUnion = (JCTree.JCTypeUnion) exType;
                        for (JCTree.JCExpression alternative : typeUnion.alternatives) {
                            args = args.append(buildCatch(alternative, catchContext));
                        }
                    } else {
                        args = args.append(buildCatch(exType, catchContext));
                    }
                }
                bodyTree.expr = safeMaker().Apply(
                        List.nil(),
                        symbols.makePromiseDoCatch(bodyTree.expr),
                        List.of(
                                safeMaker().Apply(
                                        List.nil(),
                                        symbols.makeCatchersOf(),
                                        args.toList()
                                )
                        )
                );
            }
            if (finallyContext != null) {
                TransMethodContext methodContext = getEnclosingMethodContext();
                bodyTree.expr = safeMaker().Apply(
                        List.nil(),
                        symbols.makePromiseDoFinally(bodyTree.expr),
                        List.of(
                                methodContext.makeVoidPromiseSupplier(finallyContext)
                        )
                );
            }
            return bodyTree;
        } else {
            tree.resources = JavacUtils.mapList(resourceContexts.toList(), ctx -> ctx.buildTree(false));
            tree.body = (JCTree.JCBlock) bodyContext.buildTree(false);
            tree.catchers = JavacUtils.mapList(catchContexts.toList(), ctx -> (JCTree.JCCatch) ctx.buildTree(false));
            tree.finalizer = finallyContext != null ? (JCTree.JCBlock) finallyContext.buildTree(false) : null;
            return tree;
        }
    }
}
