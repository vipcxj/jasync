package io.github.vipcxj.jasync.core.javac;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacScope;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import io.github.vipcxj.jasync.core.javac.model.JAsyncInfo;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

public interface IJAsyncInstanceContext extends IJAsyncContext {

    JAsyncInfo getInfo();

    ExecutableElement getMethodRoot();

    CompilationUnitTree getCompilationUnitTree();

    TreePath getPath(JCTree tree);

    JavacScope getScope(JCTree tree);

    Element getElement(JCTree tree);

    TreeMaker safeMaker();
}
