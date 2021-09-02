package io.github.vipcxj.jasync.core.javac.translate;

import com.sun.tools.javac.tree.JCTree;

public interface TransDecorator {
    int DEFAULT_ORDER = 0;
    JCTree decorate(TranslateContext<?> context, JCTree tree);
}
