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
import io.github.vipcxj.jasync.core.javac.translate.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class AbstractTranslateContext<T extends JCTree> implements TranslateContext<T> {
    protected final AnalyzerContext analyzerContext;
    private TranslateContext<?> parent;
    private final List<TranslateContext<?>> children;
    protected T tree;
    protected TransBlockContext thenContext;
    protected boolean entered;
    protected boolean exited;
    protected boolean hasAwait;
    protected boolean full;
    protected boolean complete;
    protected boolean then;
    protected JCTree awaitContainer;
    protected final boolean synthetic;
    protected final List<TransCallback> preEnterCallbacks;
    protected final List<TransCallback> postEnterCallbacks;
    protected final List<TransCallback> preExitCallbacks;
    protected final List<TransCallback> postExitCallbacks;
    protected final List<DecoratorBox> decorators;

    public AbstractTranslateContext(AnalyzerContext analyzerContext, T tree, boolean synthetic) {
        this.analyzerContext = analyzerContext;
        this.tree = tree;
        this.children = new ArrayList<>();
        this.hasAwait = false;
        this.full = false;
        this.entered = false;
        this.exited = false;
        this.then = false;
        this.synthetic = synthetic;
        this.preEnterCallbacks = new ArrayList<>();
        this.postEnterCallbacks = new ArrayList<>();
        this.preExitCallbacks = new ArrayList<>();
        this.postExitCallbacks = new ArrayList<>();
        this.decorators = new ArrayList<>();
    }

    public AbstractTranslateContext(AnalyzerContext analyzerContext, T tree) {
        this(analyzerContext, tree, false);
    }

    @Override
    public JCTree awaitContainer() {
        return awaitContainer;
    }

    @Override
    public void setAwaitContainer(JCTree awaitContainer) {
        this.awaitContainer = awaitContainer;
    }

    @Override
    public void addPostEnterTrigger(TransCallback trigger) {
        postEnterCallbacks.add(trigger);
    }

    @Override
    public void removePostEnterTrigger(TransCallback trigger) {
        postEnterCallbacks.remove(trigger);
    }

    @Override
    public void addPostExitTrigger(TransCallback trigger) {
        postExitCallbacks.add(trigger);
    }

    @Override
    public void removePostExitTrigger(TransCallback trigger) {
        postExitCallbacks.remove(trigger);
    }

    @Override
    public void addPreEnterTrigger(TransCallback trigger) {
        preEnterCallbacks.add(trigger);
    }

    @Override
    public void removePreEnterTrigger(TransCallback trigger) {
        preEnterCallbacks.remove(trigger);
    }

    @Override
    public void addPreExitTrigger(TransCallback trigger) {
        preExitCallbacks.add(trigger);
    }

    @Override
    public void removePreExitTrigger(TransCallback trigger) {
        preExitCallbacks.remove(trigger);
    }

    @Override
    public void addDecorator(TransDecorator decorator, int order) {
        decorators.add(new DecoratorBox(decorator, order));
        decorators.sort(Comparator.comparingInt(DecoratorBox::getOrder));
    }

    protected JCTree decorate(JCTree tree) {
        for (DecoratorBox decorator : decorators) {
            tree = decorator.getDecorator().decorate(this, tree);
        }
        return tree;
    }

    protected void processAwait() {
        JCTree container = awaitContainer();
        if (container != null) {
            IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
            AwaitContext awaitContext = AwaitContext.scan(jasyncContext, this);
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

    @Override
    public TranslateContext<T> enter() {
        return enter(true);
    }

    @Override
    public TranslateContext<T> enter(boolean triggerCallback) {
        if (this.entered) {
            throw new IllegalStateException("Has been entered.");
        }
        if (triggerCallback) {
            for (TransCallback callback : preEnterCallbacks) {
                callback.onTrigger(this);
            }
            if (analyzerContext.currentTranslateContext != null) {
                analyzerContext.currentTranslateContext.onChildEnter(this);
            }
        }
        processAwait();
        this.entered = true;
        this.parent = analyzerContext.currentTranslateContext;
        analyzerContext.currentTranslateContext = this;
        if (triggerCallback) {
            for (TransCallback callback : postEnterCallbacks) {
                callback.onTrigger(this);
            }
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
        if (triggerCallback) {
            for (TransCallback callback : preExitCallbacks) {
                callback.onTrigger(this);
            }
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
        if (triggerCallback) {
            for (TransCallback callback : postExitCallbacks) {
                callback.onTrigger(this);
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
    public boolean hasAwait() {
        return hasAwait;
    }

    @Override
    public void setHasAwait(boolean hasAwait) {
        this.hasAwait = hasAwait;
    }

    @Override
    public boolean isSynthetic() {
        return synthetic;
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
                newTree = (JCTree.JCReturn) decorate(newTree);
                if (replaceSelf) {
                    replaceBy(newTree);
                }
                return newTree;
            } finally {
                maker.pos = prePos;
            }
        } else {
            return decorate(buildTreeWithoutThen(replaceSelf));
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
        if (!this.then) {
            this.then = true;
            analyzerContext.currentTranslateContext = this;
            thenContext = new TransBlockContext(analyzerContext, null, true);
            thenContext.setHasAwait(true);
            thenContext.setNude(true);
            thenContext.enter();
        }
    }

    @Override
    public void endThen() {
        if (!then) {
            throw new IllegalStateException("Call startThen first.");
        }
        then = false;
        thenContext.exit();
    }

    @Override
    public boolean inThen() {
        return then;
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
            checkContextTree(wrapBlockContext.getWrappedContext(), tree);
            return;
        }
        if (context instanceof TransAwaitContext) {
            checkContextTree(((TransAwaitContext) context).getTargetContext(), tree);
            return;
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

    protected boolean isDebug() {
        return analyzerContext.isDebug();
    }

    static class DecoratorBox {
        private final TransDecorator decorator;
        private final int order;

        public DecoratorBox(TransDecorator decorator, int order) {
            this.decorator = decorator;
            this.order = order;
        }

        public TransDecorator getDecorator() {
            return decorator;
        }

        public int getOrder() {
            return order;
        }
    }
}
