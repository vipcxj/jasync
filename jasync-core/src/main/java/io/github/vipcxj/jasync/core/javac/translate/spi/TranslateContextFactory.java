package io.github.vipcxj.jasync.core.javac.translate.spi;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;

public interface TranslateContextFactory {

    TranslateContext<?> create(AnalyzerContext context, JCTree tree);

    boolean isSupport(JCTree tree);

    int priority();
}
