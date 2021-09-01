package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import io.github.vipcxj.jasync.core.javac.Constants;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.context.JAsyncSymbols;
import io.github.vipcxj.jasync.core.javac.model.AwaitContext;
import io.github.vipcxj.jasync.core.javac.model.Frame;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;
import io.github.vipcxj.jasync.core.javac.visitor.JAsyncAnalyzer;

import java.util.HashSet;
import java.util.Set;

public class TransAwaitContext extends AbstractTransExpressionContext<JCTree.JCMethodInvocation> {

    private JCTree container;
    private ListBuffer<JCTree.JCVariableDecl> proxyDecls;
    private ListBuffer<TranslateContext<?>> exprContexts;
    private TranslateContext<?> awaitContext;
    private final Set<JCTree> toReplaced;
    private ChildState childState;

    public TransAwaitContext(AnalyzerContext analyzerContext, JCTree container, AwaitContext.AwaitPart awaitPart) {
        super(analyzerContext, awaitPart.getAwaitInvoker());
        this.container = container;
        this.hasAwait = true;
        this.toReplaced = new HashSet<>();
        this.proxyDecls = new ListBuffer<>();
        this.exprContexts = new ListBuffer<>();
    }

    public JCTree getContainer() {
        return container;
    }

    public boolean isExpr() {
        return container instanceof JCTree.JCExpression;
    }

    public ListBuffer<JCTree.JCVariableDecl> getProxyDecls() {
        return proxyDecls;
    }

    @Override
    public TransAwaitContext enter(boolean triggerCallback) {
        super.enter(triggerCallback);
        return this;
    }



    @Override
    protected void addNormalChildContext(TranslateContext<?> child) {
        if (childState == ChildState.EXPR) {
            exprContexts = exprContexts.append(child);
        } else if (childState == ChildState.AWAIT) {
            awaitContext = child;
        } else {
            throwIfFull();
        }
    }

    @Override
    public boolean hasAwait() {
        return true;
    }

    public boolean shouldBeReplaced(JCTree tree) {
        return toReplaced.contains(tree);
    }

    private Frame.PlaceHolderInfo getPlaceHolder(JCTree tree) {
        return getFrame().getDeclaredPlaceHolders().get(tree);
    }

    public static void make(AnalyzerContext analyzerContext, AwaitContext awaitContext) {
        for (AwaitContext.AwaitPart awaitPart : awaitContext.getAwaitParts()) {
            TransAwaitContext transAwaitContext = new TransAwaitContext(analyzerContext, awaitContext.getContainer(), awaitPart);
            transAwaitContext.enter();
            try {
                for (JCTree.JCExpression expression : awaitPart.getExpressions()) {
                    transAwaitContext.childState = ChildState.EXPR;
                    new JAsyncAnalyzer(analyzerContext).scan(expression);
                    transAwaitContext.toReplaced.add(expression);
                }
                transAwaitContext.childState = ChildState.AWAIT;
                new JAsyncAnalyzer(analyzerContext).scan(transAwaitContext.tree);
                transAwaitContext.toReplaced.add(transAwaitContext.tree);
            } finally {
                transAwaitContext.childState = ChildState.COMPLETE;
                transAwaitContext.exit();
            }
            transAwaitContext.startThen();
        }
    }

    private JCTree.JCExpression makeAwaitThen() {
        IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
        JAsyncSymbols symbols = jasyncContext.getJAsyncSymbols();
        JCTree.JCMethodInvocation awaitTree = (JCTree.JCMethodInvocation) awaitContext.buildTree(false);
        JCTree.JCExpression method = awaitTree.meth;
        if (isExpr()) {
            if (method instanceof JCTree.JCFieldAccess) {
                return symbols.makePromiseThenFuncArg(((JCTree.JCFieldAccess) method).getExpression());
            } else {
                return symbols.makePromiseThenFuncArg(null);
            }
        } else {
            if (method instanceof JCTree.JCFieldAccess) {
                return symbols.makePromiseThenVoidFuncArg(((JCTree.JCFieldAccess) method).getExpression());
            } else {
                return symbols.makePromiseThenVoidFuncArg(null);
            }
        }
    }

    @Override
    public void complete() {
        for (TranslateContext<?> exprContext : exprContexts) {
            exprContext.complete();
            analyzerContext.addPlaceHolder(exprContext.getTree(), false);
        }
        awaitContext.complete();
        if (thenContext != null) {
            thenContext.complete();
        }
    }

    @Override
    public JCTree buildTree(boolean replaceSelf) {
        IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
        TreeMaker maker = jasyncContext.getTreeMaker();
        TransMethodContext methodContext = getEnclosingMethodContext();
        List<TranslateContext<?>> exprContextList = exprContexts.toList();
        for (TranslateContext<?> exprContext : exprContextList) {
            Frame.PlaceHolderInfo placeHolder = getPlaceHolder(exprContext.getTree());
            JCTree.JCExpression expr = (JCTree.JCExpression) exprContext.buildTree(false);
            int prePos = maker.pos;
            try {
                proxyDecls = proxyDecls.append(safeMaker().VarDef(placeHolder.getSymbol(), expr));
            } finally {
                maker.pos = prePos;
            }
        }
        int prePos = maker.pos;
        try {
            JCTree.JCMethodDecl methodDecl = isExpr()
                    ? methodContext.addPromiseFunction(thenContext, getContainer().type)
                    : methodContext.addVoidPromiseFunction(thenContext);
            JCTree.JCReturn outTree = maker.Return(
                    maker.Apply(
                            List.nil(),
                            makeAwaitThen(),
                            List.of(methodContext.makeFunctional(
                                    thenContext.getFrame(),
                                    isExpr() ? Constants.INDY_MAKE_PROMISE_FUNCTION : Constants.INDY_MAKE_VOID_PROMISE_FUNCTION,
                                    methodDecl))
                    )
            );
            return decorate(outTree);
        } finally {
            maker.pos = prePos;
        }
    }

    enum ChildState {
        EXPR, AWAIT, COMPLETE
    }
}
