package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;

public class TransDefaultContext<T extends JCTree> extends AbstractTranslateContext<T> {

    public TransDefaultContext(AnalyzerContext analyzerContext, T tree) {
        super(analyzerContext, tree);
    }

}
