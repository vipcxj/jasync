package io.github.vipcxj.jasync.core.javac.model;

import com.sun.tools.javac.tree.JCTree;

import java.util.ArrayList;
import java.util.List;

public class AstTree {

    public AstNode root;
    public AstNode pointer;

    public AstTree() {
    }

    public void dls(NodeVisitor visitor, NodePruner pruner) {
        AstTree.dls(root, visitor, pruner);
    }

    /**
     * 1. 子树从左向右遍历
     * 2. 同一子树，节点由深往浅遍历
     */
    public static void dls(AstNode root, NodeVisitor visitor, NodePruner pruner) {
        if (root == null) {
            return;
        }
        if (pruner != null && pruner.prune(root)) {
            return;
        }
        for (AstNode child : root.children) {
            dls(child, visitor, pruner);
        }
        visitor.visit(root);
    }

    public static AstNode findNode(AstNode root, JCTree tree) {
        if (root == null) {
            return null;
        }
        if (root.tree == tree) {
            return root;
        }
        for (AstNode child : root.children) {
            AstNode node = findNode(child, tree);
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    public void enter(JCTree tree) {
        if (pointer == null) {
            root = pointer = new AstNode(null, tree);
        } else {
            AstNode newNode = new AstNode(pointer, tree);
            pointer.children.add(newNode);
            pointer = newNode;
        }
    }

    public void exit() {
        if (pointer == null) {
            throw new IllegalStateException("Point to nothing.");
        }
        pointer = pointer.parent;
    }

    public static class AstNode {
        public AstNode parent;
        public List<AstNode> children;
        public JCTree tree;

        public AstNode(AstNode parent, JCTree tree) {
            this.parent = parent;
            this.tree = tree;
            this.children = new ArrayList<>();
        }

        /**
         * check whether self is the ancestor of the node.
         * @param node the node to test
         * @return the result
         */
        public boolean isAncestor(AstNode node) {
            if (node == null || node == this) {
                return false;
            }
            if (node.parent == this) {
                return true;
            }
            if (node.parent != null) {
                return isAncestor(node.parent);
            } else {
                return false;
            }
        }
    }

    interface NodeVisitor {
        void visit(AstNode node);
    }

    interface NodePruner {
        boolean prune(AstNode node);
    }
}
