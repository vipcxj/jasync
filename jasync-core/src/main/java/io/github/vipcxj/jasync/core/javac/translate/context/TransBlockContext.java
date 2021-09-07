package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import io.github.vipcxj.jasync.core.javac.Constants;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.context.JAsyncSymbols;
import io.github.vipcxj.jasync.core.javac.model.Frame;
import io.github.vipcxj.jasync.core.javac.model.VarKey;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;

import java.util.ArrayDeque;
import java.util.Deque;

public class TransBlockContext extends AbstractTransFrameHolderStatementContext<JCTree.JCBlock> {
    private final Deque<TranslateContext<?>> children;
    private boolean nude;
    private boolean direct;

    public TransBlockContext(AnalyzerContext analyzerContext, JCTree.JCBlock tree) {
        super(analyzerContext, tree);
        this.children = new ArrayDeque<>();
        this.nude = false;
        this.direct = false;
    }

    public void setNude(boolean nude) {
        this.nude = nude;
    }

    public boolean isDirect() {
        return direct;
    }

    public void setDirect(boolean direct) {
        this.direct = direct;
    }

    public boolean innerHasAwait() {
        TranslateContext<?> lastContext = children.peekLast();
        return lastContext != null && lastContext.hasAwait();
    }

    public TranslateContext<?> getSingleChild() {
        if (children.size() == 1) {
            return children.peekFirst();
        }
        throw new IllegalArgumentException("Only can be called when single statement in block");
    }

    @Override
    public TransBlockContext enter() {
        super.enter();
        return this;
    }

    @Override
    public void exit(boolean triggerCallback) {
        TranslateContext<?> lastContext = children.peekLast();
        if (lastContext != null && lastContext.hasAwait()) {
            lastContext.endThen();
        }
        super.exit(triggerCallback);
    }

    @Override
    protected void addNormalChildContext(TranslateContext<?> child) {
        if (child.hasAwait()) {
            TranslateContext<?> last = children.peekLast();
            if (last != null && last.hasAwait()) {
                throw new IllegalStateException("Promise context has been set.");
            }
            children.offer(child);
            child.startThen();
        } else {
            children.offer(child);
        }
    }

    @Override
    protected JCTree buildTreeWithoutThen(boolean replaceSelf) {
        ListBuffer<JCTree.JCStatement> stats = new ListBuffer<>();
        IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
        JAsyncSymbols symbols = jasyncContext.getJAsyncSymbols();
        if (isAwaitScope()) {
            TreeMaker maker = jasyncContext.getTreeMaker();
            for (Frame.DeclInfo declInfo : getFrame().getDeclaredVars().values()) {
                if (declInfo.isAsyncParam()) {
                    JCTree.JCVariableDecl referenceDecl = declInfo.getReferenceDecl();
                    if (referenceDecl != null) {
                        stats = stats.append(referenceDecl);
                    }
                }
            }
            for (Frame.CapturedInfo capturedInfo : getFrame().getCapturedVars().values()) {
                if (capturedInfo.isNotReadOnly()) {
                    stats = stats.append(capturedInfo.makeUsedDecl());
                }
            }
            if (isDebug()) {
                for (Frame.CapturedInfo capturedInfo : getFrame().getDebugCapturedVars().values()) {
                    if (capturedInfo.isNotReadOnly()) {
                        stats = stats.append(capturedInfo.makeUsedDecl());
                    }
                }
            }
            for (TranslateContext<?> child : children) {
                JCTree tree = child.buildTree(false);
                if (tree == null) {
                    continue;
                }
                JCTree.JCStatement statement;
                if (tree instanceof JCTree.JCExpression) {
                    if (child.hasAwait()) {
                        statement = JavacUtils.makeReturn(jasyncContext, (JCTree.JCExpression) tree);
                    } else if (hasAwait()) {
                        statement = JavacUtils.makeReturn(jasyncContext, symbols.makeJust((JCTree.JCExpression) tree));
                    } else {
                        statement = JavacUtils.makeReturn(jasyncContext, (JCTree.JCExpression) tree);
                    }
                } else {
                    statement = (JCTree.JCStatement) tree;
                }
                if (child instanceof TransAwaitContext) {
                    for (JCTree.JCVariableDecl decl : ((TransAwaitContext) child).getProxyDecls().toList()) {
                        stats = stats.append(decl);
                    }
                }
                if (child.hasAwait()) {
                    assert child == children.getLast();
                    if (direct) {
                        assert statement instanceof JCTree.JCReturn;
                        JCTree.JCReturn jcReturn = (JCTree.JCReturn) statement;
                        jcReturn.expr = symbols.makeCatchReturn(jcReturn.expr);
                    }
                }
                stats = stats.append(statement);
                if (child instanceof TransVarDeclContext) {
                    JCTree.JCVariableDecl decl = (JCTree.JCVariableDecl) child.getTree();
                    Frame.DeclInfo declInfo = getFrame().getDeclaredVars().get(new VarKey(decl.sym));
                    JCTree.JCVariableDecl referenceDecl = declInfo.getReferenceDecl();
                    if (referenceDecl != null) {
                        stats = stats.append(referenceDecl);
                    }
                }
            }
            if (tree == null) {
                tree = JavacUtils.makeBlock(jasyncContext, stats.toList());
            } else {
                tree.stats = stats.toList();
            }
            if (!nude) {
                TransMethodContext methodContext = getEnclosingMethodContext();
                JCTree.JCMethodDecl methodDecl = methodContext.addVoidPromiseSupplier(getFrame(), tree);
                int prePos = maker.pos;
                try {
                    maker.pos = tree.pos;
                    return JavacUtils.makeReturn(jasyncContext, maker.Apply(
                            List.nil(),
                            symbols.makeJAsyncDeferVoid(),
                            List.of(
                                    methodContext.makeFunctional(getFrame(), Constants.INDY_MAKE_VOID_PROMISE_SUPPLIER, methodDecl),
                                    makeLabelArg()
                            )
                    ));
                } finally {
                    maker.pos = prePos;
                }
            } else {
                return tree;
            }
        } else {
            for (TranslateContext<?> child : children) {
                JCTree tree = child.buildTree(false);
                if (tree == null) {
                    continue;
                }
                JCTree.JCStatement statement;
                if (tree instanceof JCTree.JCExpression) {
                    statement = JavacUtils.makeReturn(jasyncContext, (JCTree.JCExpression) tree);
                } else {
                    statement = (JCTree.JCStatement) tree;
                }
                stats = stats.append(statement);
            }
            if (tree == null) {
                tree = JavacUtils.makeBlock(jasyncContext, stats.toList());
            } else {
                tree.stats = stats.toList();
            }
            return tree;
        }
    }
}
