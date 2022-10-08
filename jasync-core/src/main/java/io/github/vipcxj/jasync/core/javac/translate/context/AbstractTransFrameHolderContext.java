package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.model.Frame;
import io.github.vipcxj.jasync.core.javac.translate.TransFrameHolderContext;

public class AbstractTransFrameHolderContext<T extends JCTree>
        extends AbstractTranslateContext<T>
        implements TransFrameHolderContext<T> {

    private Frame frame;
    private boolean awaitScope;

    public AbstractTransFrameHolderContext(AnalyzerContext analyzerContext, T tree) {
        super(analyzerContext, tree);
    }

    @Override
    public Frame getFrame() {
        return frame;
    }

    @Override
    public void setHasAwait(boolean hasAwait) {
        super.setHasAwait(hasAwait);
        if (hasAwait) {
            setAwaitScope(true);
        }
    }

    @Override
    public boolean isAwaitScope() {
        return awaitScope;
    }

    @Override
    public void setAwaitScope(boolean awaitScope) {
        this.awaitScope = awaitScope;
    }

    @Override
    public void complete() {
        Frame preFrame = analyzerContext.enter(this);
        frame = analyzerContext.currentFrame();
        frame.markOrder();
        try {
            super.complete(false);
        } finally {
            analyzerContext.exitTo(preFrame);
        }
        if (thenContext != null) {
            thenContext.complete();
        }
    }
}