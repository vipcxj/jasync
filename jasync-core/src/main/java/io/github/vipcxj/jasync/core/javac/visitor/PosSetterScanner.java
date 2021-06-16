package io.github.vipcxj.jasync.core.javac.visitor;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;

public class PosSetterScanner extends TreeScanner {

    private final int pos;

    public PosSetterScanner(int pos) {
        this.pos = pos;
    }

    @Override
    public void scan(JCTree tree) {
        tree.pos = pos;
        super.scan(tree);
    }
}
