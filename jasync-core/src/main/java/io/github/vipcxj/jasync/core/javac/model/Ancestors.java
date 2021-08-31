package io.github.vipcxj.jasync.core.javac.model;


import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;

public class Ancestors {
    private List<JCTree> trees;

    public Ancestors() {
        this.trees = List.nil();
    }

    public Ancestors(List<JCTree> trees) {
        this.trees = trees;
    }

    public Ancestors copy() {
        return new Ancestors(trees);
    }

    public void enter(JCTree tree) {
        trees = trees.prepend(tree);
    }

    public void exit() {
        trees = trees.tail;
    }

    public <T extends JCTree> T ancestor(Class<T> type) {
        for (List<? extends JCTree> l = trees; l.nonEmpty(); l = l.tail)
            if (type.isAssignableFrom(l.head.getClass())) {
                //noinspection unchecked
                return (T) l.head;
            }
        return null;
    }

    public JCTree.JCStatement enclosingStatement() {
        return ancestor(JCTree.JCStatement.class);
    }

    public JCTree.JCMethodDecl enclosingMethod() {
        return ancestor(JCTree.JCMethodDecl.class);
    }

    public JCTree.JCClassDecl enclosingClass() {
        return ancestor(JCTree.JCClassDecl.class);
    }

    public JCTree current() {
        return trees != null ? trees.head : null;
    }

    public JCTree parent() {
        return trees != null && trees.tail != null ? trees.tail.head : null;
    }

    public JCTree.JCBlock directBlock() {
        JCTree blockMaybe = parent();
        return blockMaybe instanceof JCTree.JCBlock ? (JCTree.JCBlock) blockMaybe : null;
    }

    public boolean isDirectInBlock() {
        return parent() instanceof JCTree.JCBlock;
    }

    public boolean insertBefore(List<JCTree.JCStatement> statements) {
        JCTree.JCBlock block = directBlock();
        if (block != null) {
            if (block.stats == null) {
                block.stats = List.nil();
            }
            if (block.stats.head == trees.head) {
                block.stats = block.stats.prependList(statements);
            } else {
                for (List<JCTree.JCStatement> l = block.stats; l.tail != null && !l.tail.isEmpty(); l = l.tail) {
                    if (l.tail.head == trees.head) {
                        l.tail = l.tail.prependList(statements);
                        break;
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean contains(JCTree tree) {
        for (List<? extends JCTree> l = trees; l.nonEmpty(); l = l.tail)
            if (l.head == tree) {
                return true;
            }
        return false;
    }
}
