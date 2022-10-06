package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.context.JAsyncSymbols;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;

import java.util.Objects;

public class TransForContext extends AbstractTransStatementContext<JCTree.JCForLoop> {

    private ChildState childState;
    private TransBlockContext initContext;
    private TransBlockContext condContext;
    private TransBlockContext stepContext;
    private TransBlockContext bodyContext;
    private final boolean hasCond;
    private final boolean hasStep;
    private final boolean hasBody;

    public TransForContext(AnalyzerContext analyzerContext, JCTree.JCForLoop tree) {
        super(analyzerContext, tree);
        boolean hasInit = tree.init != null && !tree.init.isEmpty();
        this.hasCond = tree.cond != null;
        this.hasStep = tree.step != null && !tree.step.isEmpty();
        this.hasBody = tree.body != null;
        this.childState = hasInit
                ? ChildState.INIT
                : hasCond
                ? ChildState.COND
                : hasStep
                ? ChildState.STEP
                : hasBody
                ? ChildState.BODY
                : ChildState.COMPLETE;
    }

    @Override
    protected void addNormalChildContext(TranslateContext<?> child) {
        ChildState noCond = hasStep
                ? ChildState.STEP
                : hasBody
                ? ChildState.BODY
                : ChildState.COMPLETE;
        if (this.childState == ChildState.INIT) {
            childContextMustBeBlock(child);
            this.initContext = (TransBlockContext) child;
            this.initContext.setNude(true);
            this.childState = hasCond
                    ? ChildState.COND
                    : noCond;
        } else if (this.childState == ChildState.COND) {
            childContextMustBeBlock(child);
            this.condContext = (TransBlockContext) child;
            this.condContext.setNude(true);
            this.childState = noCond;
        } else if (this.childState == ChildState.STEP) {
            childContextMustBeBlock(child);
            this.stepContext = (TransBlockContext) child;
            this.stepContext.setNude(true);
            this.childState = hasBody
                    ? ChildState.BODY
                    : ChildState.COMPLETE;
        } else if (this.childState == ChildState.BODY) {
            childContextMustBeBlock(child);
            this.bodyContext = (TransBlockContext) child;
            this.bodyContext.setNude(true);
            this.childState = ChildState.COMPLETE;
        } else {
            throwIfFull();
        }
    }

    @Override
    public void exit(boolean triggerCallback) {
        if (hasAwait()) {
            if (initContext != null) {
                initContext.setAwaitScope(true);
            }
            if (condContext != null) {
                condContext.setAwaitScope(true);
            }
            if (stepContext != null) {
                stepContext.setAwaitScope(true);
            }
            if (bodyContext != null) {
                bodyContext.setAwaitScope(true);
            }
        } else {
            if (initContext != null) {
                initContext.setProxyFrame(true);
            }
            if (condContext != null) {
                condContext.setProxyFrame(true);
            }
            if (stepContext != null) {
                stepContext.setProxyFrame(true);
            }
        }
        super.exit(triggerCallback);
    }

    @Override
    protected JCTree buildTreeWithoutThen(boolean replaceSelf) {
        if (hasAwait()) {
            IJAsyncInstanceContext jasyncContext = getContext().getJasyncContext();
            TransMethodContext methodContext = getEnclosingMethodContext();
            JAsyncSymbols symbols = jasyncContext.getJAsyncSymbols();
            TreeMaker maker = jasyncContext.getTreeMaker();
            int prePos = maker.pos;
            try {
                JCTree.JCExpression initArg = initContext != null
                        ? methodContext.makeVoidPromiseSupplier(initContext)
                        : safeMaker().Literal(TypeTag.BOT, null);
                JCTree.JCExpression methodExpr = condContext != null && condContext.innerHasAwait()
                        ? symbols.makeJAsyncDoPromiseFor()
                        : symbols.makeJAsyncDoFor();
                JCTree.JCExpression condArg = condContext != null
                        ? methodContext.makeCondSupplier(condContext)
                        : safeMaker().TypeCast(
                                safeMaker().Type(symbols.getBooleanSupplierType()),
                                safeMaker().Literal(TypeTag.BOT, null)
                        );
                JCTree.JCExpression stepArg = stepContext != null
                        ? methodContext.makeVoidPromiseSupplier(stepContext)
                        : safeMaker().Literal(TypeTag.BOT, null);
                JCTree.JCExpression bodyArg = bodyContext != null
                        ? methodContext.makeVoidPromiseSupplier(bodyContext)
                        : safeMaker().Literal(TypeTag.BOT, null);
                return JavacUtils.makeReturn(jasyncContext, safeMaker().Apply(
                        List.nil(),
                        methodExpr,
                        List.of(
                                initArg,
                                condArg,
                                stepArg,
                                bodyArg,
                                makeLabelArg()
                        )
                ));
            } finally {
                maker.pos = prePos;
            }
        } else {
            tree.init = initContext != null ? JavacUtils.toListBuffer(
                    initContext.getChildren().stream()
                            .map(ctx -> (JCTree.JCStatement) ctx.buildTree(false))
                            .filter(Objects::nonNull)
            ).toList() : List.nil();
            tree.cond = condContext != null ? (JCTree.JCExpression) condContext.getSingleChild().buildTree(false) : null;
            tree.step = stepContext != null ? JavacUtils.toListBuffer(
                    stepContext.getChildren().stream()
                            .map(ctx -> (JCTree.JCExpressionStatement) ctx.buildTree(false))
                            .filter(Objects::nonNull)
            ).toList() : List.nil();
            tree.body = bodyContext != null ? (JCTree.JCBlock) bodyContext.buildTree(false) : null;
            return tree;
        }
    }

    enum ChildState {
        INIT, COND, STEP, BODY, COMPLETE
    }
}
