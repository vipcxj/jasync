package io.github.vipcxj.jasync.core.javac.translate.factories;

import com.google.auto.service.AutoService;
import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;
import io.github.vipcxj.jasync.core.javac.translate.context.TransMethodContext;
import io.github.vipcxj.jasync.core.javac.translate.spi.TranslateContextFactory;

@AutoService(TranslateContextFactory.class)
public class TransMethodContextFactory extends AbstractTranslateContextFactory {

    @Override
    public TranslateContext<?> create(AnalyzerContext context, JCTree tree) {
        return new TransMethodContext(context, (JCTree.JCMethodDecl) tree);
    }

    @Override
    public boolean isSupport(JCTree tree) {
        return tree instanceof JCTree.JCMethodDecl;
    }
}
