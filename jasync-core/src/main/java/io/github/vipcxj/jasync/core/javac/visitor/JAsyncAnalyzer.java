package io.github.vipcxj.jasync.core.javac.visitor;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;
import io.github.vipcxj.jasync.core.javac.translate.context.TransMethodContext;
import io.github.vipcxj.jasync.core.javac.translate.context.TransNullContext;
import io.github.vipcxj.jasync.core.javac.translate.context.TransTreePlaceHolderContext;
import io.github.vipcxj.jasync.core.javac.translate.factories.TranslateContextFactories;
import io.github.vipcxj.jasync.core.javac.translate.spi.TranslateContextFactory;

public class JAsyncAnalyzer extends TreeScanner {

    private final AnalyzerContext context;
    private TranslateContext<?> result;

    public JAsyncAnalyzer(IJAsyncInstanceContext context) {
        this.context = new AnalyzerContext(context);
    }

    public JAsyncAnalyzer(AnalyzerContext context) {
        this.context = context;
    }

    @Override
    public void scan(JCTree tree) {
        if (tree == null) {
            new TransNullContext(context).enter().exit();
        } else {
            if (context.shouldBeReplaced(tree)) {
                TransTreePlaceHolderContext.createContext(context, tree).enter().exit();
            } else {
                TranslateContextFactory factory = TranslateContextFactories.INSTANCE.getFactory(tree);
                TranslateContext<?> translateContext = factory.create(context, tree).enter();
                try {
                    super.scan(tree);
                } finally {
                    result = translateContext;
                    translateContext.exit();
                }
            }
        }
    }

    public static TransMethodContext scan(IJAsyncInstanceContext context, JCTree.JCMethodDecl methodDecl) {
        JAsyncAnalyzer analyzer = new JAsyncAnalyzer(context);
        analyzer.scan(methodDecl);
        return (TransMethodContext) analyzer.result;
    }
}
