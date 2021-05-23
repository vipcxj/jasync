package io.github.vipcxj.jasync.javac;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;

public class JavacTranslator extends TreeTranslator {

    private JavacContext context;

    @Override
    public void visitSelect(JCTree.JCFieldAccess tree) {
        super.visitSelect(tree);
    }
}
