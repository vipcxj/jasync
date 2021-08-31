package io.github.vipcxj.jasync.core.javac.visitor;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import io.github.vipcxj.jasync.core.javac.model.AstTree;

public class CollectTreeScanner extends TreeScanner {

    private AstTree astTree = new AstTree();

    @Override
    public void scan(JCTree tree) {
        astTree.enter(tree);
        try {
            super.scan(tree);
        } finally {
            astTree.exit();
        }
    }

    public AstTree getAstTree() {
        return astTree;
    }
}
