package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;
import io.github.vipcxj.jasync.core.javac.Constants;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.translate.TransStatementContext;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;
import io.github.vipcxj.jasync.spec.JAsync;

import javax.lang.model.util.Elements;

public class TransIfContext extends AbstractTransFrameHolderStatementContext<JCTree.JCIf> {

    private ChildState childState = ChildState.COND;
    private TranslateContext<?> condContext;
    private TransStatementContext<?> thenContext;
    private TransStatementContext<?> elseContext;

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
    public void onChildEnter(TranslateContext<?> child) {
        super.onChildEnter(child);
        if (childState == ChildState.THEN || childState == ChildState.ELSE) {
            if (child instanceof TransBlockContext || child instanceof TransIfContext) {
                return;
            }
            new TransWrapBlockContext(analyzerContext).enter(false);
        }
    }

    private void childContextMustBeIfOrBlock(TranslateContext<?> child) {
        if (child instanceof TransBlockContext || child instanceof TransIfContext) {
            return;
        }
        throw new IllegalArgumentException("The child context must be if or block context.");
    }

    @Override
    protected void addNormalChildContext(TranslateContext<?> child) {
        if (condContext == null) {
            checkContextTree(child, tree.cond);
            condContext = child;
            childState = ChildState.THEN;
        } else if (thenContext == null) {
            childContextMustBeIfOrBlock(child);
            checkContextTree(child, tree.thenpart);
            thenContext = (TransStatementContext<?>) child;
            if (thenContext instanceof TransBlockContext) {
                ((TransBlockContext) thenContext).setNude(true);
            }
            childState = ChildState.ELSE;
        } else if (elseContext == null) {
            childContextMustBeIfOrBlock(child);
            checkContextTree(child, tree.elsepart);
            elseContext = (TransStatementContext<?>) child;
            if (elseContext instanceof TransBlockContext) {
                ((TransBlockContext) elseContext).setNude(true);
            }
            childState = ChildState.COMPLETE;
        } else {
            throwIfFull();
        }
    }

    @Override
    protected JCTree buildTreeWithoutThen(boolean replaceSelf) {
        tree.cond = (JCTree.JCExpression) condContext.buildTree(false);
        tree.thenpart = (JCTree.JCStatement) thenContext.buildTree(false);
        tree.elsepart = (JCTree.JCStatement) elseContext.buildTree(false);
        if (!hasAwait()) {
            return tree;
        }
        IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
        TreeMaker maker = jasyncContext.getTreeMaker();
        Names names = jasyncContext.getNames();
        Elements elements = jasyncContext.getEnvironment().getElementUtils();
        TransMethodContext methodContext = getEnclosingMethodContext();
        JCTree.JCMethodDecl methodDecl = methodContext.addVoidPromiseSupplier(
                getFrame(),
                JavacUtils.makeBlock(jasyncContext, tree)
        );
        Symbol.TypeSymbol jAsyncSymbol = (Symbol.TypeSymbol) elements.getTypeElement(JAsync.class.getCanonicalName());
        return maker.Return(maker.Apply(
                List.nil(),
                maker.Select(maker.QualIdent(jAsyncSymbol), names.fromString(Constants.JASYNC_DEFER_VOID)),
                List.of(
                        methodContext.makeFunctional(getFrame(), Constants.INDY_MAKE_VOID_PROMISE_SUPPLIER, methodDecl)
                )
        ));
    }

    enum ChildState {
        COND, THEN, ELSE, COMPLETE
    }
}
