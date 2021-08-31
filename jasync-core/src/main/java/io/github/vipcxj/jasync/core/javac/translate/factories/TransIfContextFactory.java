package io.github.vipcxj.jasync.core.javac.translate.factories;

import com.google.auto.service.AutoService;
import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;
import io.github.vipcxj.jasync.core.javac.translate.context.TransIfContext;
import io.github.vipcxj.jasync.core.javac.translate.spi.TranslateContextFactory;

@AutoService(TranslateContextFactory.class)
public class TransIfContextFactory extends AbstractTranslateContextFactory {
    @Override
    public TranslateContext<?> create(AnalyzerContext context, JCTree tree) {
        return new TransIfContext(context, (JCTree.JCIf) tree);
    }

    @Override
    public boolean isSupport(JCTree tree) {
        return tree.hasTag(JCTree.Tag.IF);
    }
}
