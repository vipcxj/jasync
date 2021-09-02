package io.github.vipcxj.jasync.core.javac.translate;

import com.sun.tools.javac.tree.JCTree;

import javax.lang.model.element.Name;

public interface TransStatementContext<T extends JCTree.JCStatement> extends TranslateContext<T> {
    void setLabel(Name label);
    JCTree.JCLiteral makeLabelArg();
}
