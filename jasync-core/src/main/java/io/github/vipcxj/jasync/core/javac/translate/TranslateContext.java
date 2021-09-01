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
    boolean inThen();
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
    boolean hasAwait();
    void setHasAwait(boolean hasAwait);
    JCTree buildTree(boolean replace);
    AnalyzerContext getContext();
    JCTree awaitContainer();
    void setAwaitContainer(JCTree awaitContainer);
    void addPreEnterTrigger(TransCallback trigger);
    void removePreEnterTrigger(TransCallback trigger);
    void addPostEnterTrigger(TransCallback trigger);
    void removePostEnterTrigger(TransCallback trigger);
    void addPreExitTrigger(TransCallback trigger);
    void removePreExitTrigger(TransCallback trigger);
    void addPostExitTrigger(TransCallback trigger);
    void removePostExitTrigger(TransCallback trigger);
    void addDecorator(TransDecorator decorator, int order);
    default void addDecorator(TransDecorator decorator) {
        addDecorator(decorator, TransDecorator.DEFAULT_ORDER);
    }
}

