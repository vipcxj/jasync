package io.github.vipcxj.jasync.core.javac.translate;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.model.Frame;

public interface TransPlaceHolderContext<T extends JCTree> extends TranslateContext<T> {
    Frame.PlaceHolderInfo getDeclPlaceHolder();
    void setCapturedPlaceHolder(Frame.PlaceHolderInfo placeHolder);

}
