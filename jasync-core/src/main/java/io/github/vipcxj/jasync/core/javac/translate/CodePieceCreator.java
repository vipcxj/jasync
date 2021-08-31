package io.github.vipcxj.jasync.core.javac.translate;

import com.sun.tools.javac.tree.JCTree;

public interface CodePieceCreator<T extends JCTree, C extends TranslateContext<T>> {

    JCTree.JCExpression create(C context);
}
