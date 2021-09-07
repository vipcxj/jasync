package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.model.Frame;
import io.github.vipcxj.jasync.core.javac.translate.TransFrameHolderContext;

public class AbstractTransFrameHolderStatementContext<T extends JCTree.JCStatement>
        extends AbstractTransStatementContext<T>
        implements TransFrameHolderContext<T> {
    protected Frame frame;
    private boolean proxyFrame;
    private boolean awaitScope;

    public AbstractTransFrameHolderStatementContext(AnalyzerContext analyzerContext, T tree) {
        super(analyzerContext, tree);
        this.proxyFrame = false;
    }

    @Override
    public Frame getFrame() {
        return proxyFrame ? super.getFrame() : frame;
    }

    public void setProxyFrame(boolean proxyFrame) {
        this.proxyFrame = proxyFrame;
    }

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
        if (!proxyFrame) {
            Frame preFrame = analyzerContext.enter(this);
            frame = analyzerContext.currentFrame();
            frame.markOrder();
            try {
                if (getParent().getThen() == this && getParent() instanceof TransAwaitContext) {
                    TransAwaitContext awaitContext = (TransAwaitContext) getParent();
                    analyzerContext.addPlaceHolder(awaitContext.getTree(), true);
                }
                super.complete(false);
            } finally {
                analyzerContext.exitTo(preFrame);
            }
            if (thenContext != null) {
                thenContext.complete();
            }
        } else {
            super.complete();
        }
    }
}
