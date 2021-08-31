package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;

public class TransNullContext extends AbstractTranslateContext<JCTree> {

    public TransNullContext(AnalyzerContext analyzerContext) {
        super(analyzerContext, null);
    }

    @Override
    protected JCTree buildTreeWithoutThen(boolean replaceSelf) {
        return null;
    }
}
