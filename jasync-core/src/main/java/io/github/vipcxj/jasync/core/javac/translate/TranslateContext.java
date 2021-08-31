package io.github.vipcxj.jasync.core.javac.translate;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.model.Frame;

import java.util.List;

public interface TranslateContext<T extends JCTree> {

    TranslateContext<T> enter();
    TranslateContext<T> enter(boolean triggerCallback);
    void exit();
    void exit(boolean triggerCallback);
    void startThen();
    void endThen();
    void addChildContext(TranslateContext<?> child);
    void onChildEnter(TranslateContext<?> child);
    void onChildExit(TranslateContext<?> child);
    void replaceBy(JCTree newTree);
    T getTree();
    TranslateContext<?> getParent();
    TranslateContext<?> getThen();
    List<TranslateContext<?>> getChildren();
    void complete();
    Frame getFrame();
    String getIdent();
    void doIdent();
    boolean hasAwait();
    void setHasAwait(boolean hasAwait);
    JCTree buildTree(boolean replace);
    AnalyzerContext getContext();
}

