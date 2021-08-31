package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import io.github.vipcxj.jasync.core.javac.Constants;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.context.JAsyncSymbols;
import io.github.vipcxj.jasync.core.javac.model.AwaitContext;
import io.github.vipcxj.jasync.core.javac.model.Frame;
import io.github.vipcxj.jasync.core.javac.translate.TransExpressionContext;
import io.github.vipcxj.jasync.core.javac.translate.TransStatementContext;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractTranslateContext<T extends JCTree> implements TranslateContext<T> {
    protected final static String IDENT_UNIT = "    ";
    protected final AnalyzerContext analyzerContext;
    private TranslateContext<?> parent;
    private List<TranslateContext<?>> children;
    protected T tree;
    protected TransBlockContext thenContext;
    protected boolean entered;
    protected boolean exited;
    protected boolean hasAwait;
    protected boolean full;
    protected boolean complete;
    protected String ident;

    public AbstractTranslateContext(AnalyzerContext analyzerContext, T tree) {
        this.analyzerContext = analyzerContext;
        this.tree = tree;
        this.children = new ArrayList<>();
        this.hasAwait = false;
        this.full = false;
        this.entered = false;
        this.exited = false;
        this.ident = "";
    }

    protected JCTree awaitContainer() {
        return null;
    }

    protected void processAwait() {
        JCTree container = awaitContainer();
        if (container != null) {
            IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
            AwaitContext awaitContext = AwaitContext.scan(jasyncContext, container);
            if (awaitContext != null) {
                TransAwaitContext.make(analyzerContext, awaitContext);
            }
        }
    }

    @Override
    public AnalyzerContext getContext() {
        return analyzerContext;
    }

    public TransMethodContext getEnclosingMethodContext() {
        TranslateContext<?> context = this.parent;
        while (context != null) {
            if (context instanceof TransMethodContext) {
                return (TransMethodContext) context;
            }
            context = context.getParent();
        }
        throw new IllegalStateException("Unable to find the enclosing method context.");
    }

    public JCTree.JCClassDecl getEnclosingClassTree() {
        TransMethodContext methodContext = getEnclosingMethodContext();
        if (methodContext != null) {
            JCTree.JCClassDecl enclosingClassTree = methodContext.getEnclosingClassTree();
            if (enclosingClassTree != null) {
                return enclosingClassTree;
            }
        }
        throw new IllegalStateException("Unable to find the enclosing class tree.");
    }

    @Override
    public TranslateContext<T> enter() {
        return enter(true);
    }

    @Override
    public TranslateContext<T> enter(boolean triggerCallback) {
        if (this.entered) {
            throw new IllegalStateException("Has been entered.");
        }
        if (triggerCallback && analyzerContext.currentTranslateContext != null) {
            analyzerContext.currentTranslateContext.onChildEnter(this);
        }
        processAwait();
        this.entered = true;
        this.parent = analyzerContext.currentTranslateContext;
        analyzerContext.currentTranslateContext = this;
        if (this.parent != null) {
            this.ident = this.parent.getIdent();
        }
        return this;
    }

    @Override
    public void exit() {
        exit(true);
    }

    @Override
    public void exit(boolean triggerCallback) {
        checkContextMustEnter();
        if (this.exited) {
            throw new IllegalStateException("Has been exited.");
        }
        this.full = true;
        this.exited = true;
        analyzerContext.currentTranslateContext = parent;
        if (parent != null) {
            if (parent.getThen() != this) {
                parent.addChildContext(this);
            }
            if (triggerCallback) {
                parent.onChildExit(this);
            }
        }
    }

    @Override
    public void complete() {
        complete(true);
    }

    protected void complete(boolean withThen) {
        if (complete) {
            throw new IllegalStateException("Has completed.");
        }
        for (TranslateContext<?> child : children) {
            child.complete();
        }
        if (withThen && thenContext != null) {
            thenContext.complete();
        }
        complete = true;
    }

    @Override
    public T getTree() {
        return tree;
    }

    @Override
    public TranslateContext<?> getParent() {
        return parent;
    }

    @Override
    public TranslateContext<?> getThen() {
        return thenContext;
    }

    @Override
    public List<TranslateContext<?>> getChildren() {
        return children;
    }

    @Override
    public String getIdent() {
        return ident;
    }

    @Override
    public void doIdent() {
        ident += IDENT_UNIT;
    }

    @Override
    public boolean hasAwait() {
        return hasAwait;
    }

    @Override
    public void setHasAwait(boolean hasAwait) {
        this.hasAwait = hasAwait;
    }

    protected JCTree buildTreeWithoutThen(boolean replaceSelf) {
        for (TranslateContext<?> child : children) {
            child.buildTree(true);
        }
        return tree;
    }

    @Override
    public JCTree buildTree(boolean replaceSelf) {
        if (thenContext != null) {
            JCTree jcTree = buildTreeWithoutThen(false);
            if (!(jcTree instanceof JCTree.JCReturn)) {
                throw new IllegalStateException("This is impossible.");
            }
            JCTree.JCReturn newTree = (JCTree.JCReturn) jcTree;
            TransMethodContext methodContext = getEnclosingMethodContext();
            IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
            TreeMaker maker = jasyncContext.getTreeMaker();
            JAsyncSymbols symbols = jasyncContext.getJAsyncSymbols();
            JCTree.JCMethodDecl methodDecl = methodContext.addVoidPromiseSupplier(thenContext);
            int prePos = maker.pos;
            try {
                newTree.expr = maker.Apply(
                        com.sun.tools.javac.util.List.nil(),
                        symbols.makePromiseThenVoidSupplierArg(newTree.expr),
                        com.sun.tools.javac.util.List.of(
                                methodContext.makeFunctional(
                                        thenContext.getFrame(),
                                        Constants.INDY_MAKE_VOID_PROMISE_SUPPLIER,
                                        methodDecl
                                )
                        )
                );
                if (replaceSelf) {
                    replaceBy(newTree);
                }
                return newTree;
            } finally {
                maker.pos = prePos;
            }
        } else {
            return buildTreeWithoutThen(replaceSelf);
        }
    }

    protected void addNormalChildContext(TranslateContext<?> child) { }

    @Override
    public void onChildEnter(TranslateContext<?> child) {}

    @Override
    public void onChildExit(TranslateContext<?> child) { }

    @Override
    public void addChildContext(TranslateContext<?> child) {
        checkContextMustEnter();
        addNormalChildContext(child);
        children.add(child);
        if (child.hasAwait()) {
            this.hasAwait = true;
        }
    }

    @Override
    public void replaceBy(JCTree newTree) {
        if (parent != null && tree != newTree) {
            if (parent.getTree() == getTree()) {
                parent.replaceBy(newTree);
            } else {
                new ChildReplacer(tree, newTree).translate(parent.getTree());
            }
        } else {
            //noinspection unchecked
            tree = (T) newTree;
        }
    }

    @Override
    public void startThen() {
        if (thenContext == null) {
            analyzerContext.currentTranslateContext = this;
            thenContext = new TransBlockContext(analyzerContext, null);
            thenContext.setHasAwait(true);
            thenContext.setNude(true);
            thenContext.enter();
        }
    }

    @Override
    public void endThen() {
        if (thenContext == null) {
            throw new IllegalStateException("Call startThen first.");
        }
        thenContext.exit();
    }

    @Override
    public Frame getFrame() {
        return parent != null ? parent.getFrame() : null;
    }

    protected void throwIfFull() {
        throw new IllegalStateException("The context has been full. No more child can be added.");
    }

    private void checkContextMustEnter() {
        if (!this.entered) {
            throw new IllegalStateException("Call enter first.");
        }
    }

    protected void checkContextTree(TranslateContext<?> context, JCTree tree) {
        if (context.getTree() == tree) return;
        if (context instanceof TransWrapBlockContext) {
            TransWrapBlockContext wrapBlockContext = (TransWrapBlockContext) context;
            if (wrapBlockContext.getWrappedTree() == tree) {
                return;
            }
        }
        throw new IllegalArgumentException("Invalid context, the tree is not matched.");
    }

    protected void childContextMustBeVarDecl(TranslateContext<?> child) {
        if (child instanceof TransVarDeclContext) return;
        throw new IllegalArgumentException("The child context must be var decl context.");
    }

    protected void childContextMustBeStatement(TranslateContext<?> child) {
        if (child instanceof TransStatementContext) return;
        throw new IllegalArgumentException("The child context must be statement context.");
    }

    protected void childContextMustBeExpression(TranslateContext<?> child) {
        if (child instanceof TransExpressionContext) return;
        throw new IllegalArgumentException("The child context must be expression context: " + child);
    }

    protected void childContextMustBeBlock(TranslateContext<?> child) {
        if (child instanceof TransBlockContext) return;
        throw new IllegalArgumentException("The child context must be block context.");
    }

    class ChildReplacer extends TreeTranslator {

        private int depth = 0;
        private final JCTree target;
        private final JCTree newTree;

        public ChildReplacer(JCTree target, JCTree newTree) {
            this.target = target;
            this.newTree = newTree;
        }

        @Override
        public <TR extends JCTree> TR translate(TR tree) {
            if (depth++ == 0) {
                return super.translate(tree);
            } else {
                if (tree == target) {
                    JavacUtils.replaceEndPosition(analyzerContext.getJasyncContext(), tree, newTree);
                    //noinspection unchecked
                    return (TR) newTree;
                } else {
                    return tree;
                }
            }
        }
    }

    protected TreeMaker treeMaker() {
        return analyzerContext.getJasyncContext().getTreeMaker();
    }

    protected TreeMaker safeMaker() {
        return analyzerContext.getJasyncContext().safeMaker();
    }
}
