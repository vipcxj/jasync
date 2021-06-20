package io.github.vipcxj.jasync.core.javac.model;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;

public interface TreeFactory<T extends JCTree> {

    T create(IJAsyncInstanceContext context);
}
