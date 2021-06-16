package io.github.vipcxj.jasync.core.javac.model;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.IJAsyncCuContext;

public interface TreeFactory<T extends JCTree> {

    T create(IJAsyncCuContext context);
}
