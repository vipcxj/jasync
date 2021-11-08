package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.context.JAsyncSymbols;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;

public class TransIfContext extends AbstractTransStatementContext<JCTree.JCIf> {

    private ChildState childState = ChildState.COND;
    private TranslateContext<?> condContext;
    private TransBlockContext thenContext;
    private TransBlockContext elseContext;

    public TransIfContext(AnalyzerContext analyzerContext, JCTree.JCIf tree) {
        super(analyzerContext, tree);
        awaitContainer = tree.cond;
    }

    @Override
    public TransIfContext enter() {
        super.enter();
        return this;
    }

    @Override
    public void exit(boolean triggerCallback) {
        if (hasAwait()) {
            thenContext.setHasAwait(true);
            if (elseContext != null) {
                elseContext.setHasAwait(true);
            }
        }
        super.exit(triggerCallback);
    }

    @Override
    public void onChildEnter(TranslateContext<?> child) {
        super.onChildEnter(child);
        if (childState == ChildState.THEN || childState == ChildState.ELSE) {
            if (child instanceof TransBlockContext) {
                return;
            }
            new TransWrapBlockContext(analyzerContext).enter(false);
        }
    }

    @Override
    protected void addNormalChildContext(TranslateContext<?> child) {
        if (condContext == null) {
            checkContextTree(child, tree.cond);
            condContext = child;
            childState = ChildState.THEN;
        } else if (thenContext == null) {
            childContextMustBeBlock(child);
            checkContextTree(child, tree.thenpart);
            thenContext = (TransBlockContext) child;
            thenContext.setNude(true);
            childState = ChildState.ELSE;
        } else if (elseContext == null) {
            childContextMustBeBlock(child);
            checkContextTree(child, tree.elsepart);
            elseContext = (TransBlockContext) child;
            elseContext.setNude(true);
            childState = ChildState.COMPLETE;
        } else {
            throwIfFull();
        }
    }

    @Override
    protected JCTree buildTreeWithoutThen(boolean replaceSelf) {
        if (hasAwait()) {
            IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
            TreeMaker maker = jasyncContext.getTreeMaker();
            JAsyncSymbols symbols = jasyncContext.getJAsyncSymbols();
            TransMethodContext methodContext = getEnclosingMethodContext();
            int prePos = maker.pos;
            try {
                return JavacUtils.makeReturn(jasyncContext, safeMaker().Apply(
                        List.nil(),
                        symbols.makeJAsyncDoIf(),
                        List.of(
                                (JCTree.JCExpression) condContext.buildTree(false),
                                methodContext.makeVoidPromiseSupplier(thenContext),
                                elseContext != null ? methodContext.makeVoidPromiseSupplier(elseContext) : JavacUtils.makeNull(jasyncContext)
                        )
                ));
            } finally {
                maker.pos = prePos;
            }
        } else {
            tree.cond = (JCTree.JCExpression) condContext.buildTree(false);
            tree.thenpart = (JCTree.JCStatement) thenContext.buildTree(false);
            tree.elsepart = elseContext != null ? (JCTree.JCStatement) elseContext.buildTree(false) : null;
            return tree;
        }
    }

    enum ChildState {
        COND, THEN, ELSE, COMPLETE
    }
}
