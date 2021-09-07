package io.github.vipcxj.jasync.core.javac.translate;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.model.Frame;

public interface TransFrameHolderContext<T extends JCTree> extends TranslateContext<T> {
    Frame getFrame();
    boolean isAwaitScope();
    void setAwaitScope(boolean awaitScope);
}
