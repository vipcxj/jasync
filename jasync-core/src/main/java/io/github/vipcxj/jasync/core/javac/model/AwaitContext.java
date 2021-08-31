package io.github.vipcxj.jasync.core.javac.model;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;
import io.github.vipcxj.jasync.core.javac.visitor.CollectTreeScanner;

import java.util.*;


public class AwaitContext {
    private final JCTree container;
    private final List<AwaitPart> awaitParts;

    public AwaitContext(JCTree container) {
        this.container = container;
        this.awaitParts = new ArrayList<>();
    }

    public JCTree getContainer() {
        return container;
    }

    public List<AwaitPart> getAwaitParts() {
        return awaitParts;
    }

    private static boolean isJCFunctionalExpression(Class<?> type) {
        if (type == null || type == Object.class) {
            return false;
        }
        if (type.getCanonicalName().equals("com.sun.tools.javac.tree.JCTree.JCFunctionalExpression")) {
            return true;
        }
        return isJCFunctionalExpression(type.getSuperclass());
    }

    private static boolean isJCFunctionalExpression(AstTree.AstNode node) {
        return node != null && node.tree != null && isJCFunctionalExpression(node.tree.getClass());
    }

    private static boolean isNotInAssignLeftPart(AstTree.AstNode node) {
        if (node.parent != null) {
            if (node.parent.tree instanceof JCTree.JCVariableDecl) {
                JCTree.JCVariableDecl decl = (JCTree.JCVariableDecl) node.parent.tree;
                return node.tree != decl.nameexpr && node.tree != decl.vartype;
            } else if (node.parent.tree instanceof JCTree.JCAssign) {
                JCTree.JCAssign assign = (JCTree.JCAssign) node.parent.tree;
                return node.tree != assign.getVariable();
            } else if (node.parent.tree instanceof JCTree.JCAssignOp) {
                JCTree.JCAssignOp assignOp = (JCTree.JCAssignOp) node.parent.tree;
                return node.tree != assignOp.getVariable();
            } else {
                return isNotInAssignLeftPart(node.parent);
            }
        }
        return true;
    }

    private static boolean shouldBeExtracted(AstTree.AstNode node) {
        if (node.tree == null) {
            return false;
        }
        return node.tree instanceof JCTree.JCMethodInvocation
                || node.tree instanceof JCTree.JCAssign
                || node.tree.getTag().isAssignop()
                || node.tree.getTag().isIncOrDecUnaryOp();
    }

    public static AwaitContext scan(IJAsyncInstanceContext context, JCTree container) {
        CollectTreeScanner collectTreeScanner = new CollectTreeScanner();
        collectTreeScanner.scan(container);
        AstTree astTree = collectTreeScanner.getAstTree();
        List<AstTree.AstNode> nodes = new LinkedList<>();
        Set<AstTree.AstNode> awaitTrees = new HashSet<>();
        astTree.dls(node -> {
            if (node.tree == null) {
                return;
            }
            if (node.tree.hasTag(JCTree.Tag.APPLY) && JavacUtils.isAwaitTree(context, node.tree)) {
                awaitTrees.add(node);
            }
            if (shouldBeExtracted(node)) {
                ListIterator<AstTree.AstNode> iterator = nodes.listIterator(nodes.size());
                while (iterator.hasPrevious()) {
                    AstTree.AstNode previous = iterator.previous();
                    if (awaitTrees.contains(previous) || !node.isAncestor(previous)) {
                        break;
                    } else {
                        iterator.remove();
                    }
                }
                nodes.add(node);
            }
        }, node -> node.tree instanceof JCTree.JCClassDecl || node.tree instanceof JCTree.JCMethodDecl || isJCFunctionalExpression(node));
        if (awaitTrees.isEmpty()) {
            return null;
        }
        AwaitContext awaitContext = new AwaitContext(container);
        List<JCTree.JCExpression> expressions = new ArrayList<>();
        for (AstTree.AstNode node : nodes) {
            if (awaitTrees.contains(node)) {
                AwaitPart part = new AwaitPart((JCTree.JCMethodInvocation) node.tree, expressions);
                awaitContext.getAwaitParts().add(part);
                expressions = new ArrayList<>();
            } else {
                expressions.add((JCTree.JCExpression) node.tree);
            }
        }
        return awaitContext;
    }

    public static class AwaitPart {
        private final JCTree.JCMethodInvocation awaitInvoker;
        private final List<JCTree.JCExpression> expressions;

        public AwaitPart(JCTree.JCMethodInvocation awaitInvoker, List<JCTree.JCExpression> expressions) {
            this.awaitInvoker = awaitInvoker;
            this.expressions = expressions;
        }

        public JCTree.JCMethodInvocation getAwaitInvoker() {
            return awaitInvoker;
        }

        public List<JCTree.JCExpression> getExpressions() {
            return expressions;
        }
    }
}
