package io.github.vipcxj.jasync.core.javac;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacScope;
import com.sun.tools.javac.tree.JCTree;

import javax.lang.model.element.Element;

public interface IJAsyncCuContext extends IJAsyncContext {

    CompilationUnitTree getCompilationUnitTree();

    TreePath getPath(JCTree tree);

    JavacScope getScope(JCTree tree);

    Element getElement(JCTree tree);
}
