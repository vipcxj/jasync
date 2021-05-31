package io.github.vipcxj.jasync.core.javac;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacScope;
import com.sun.tools.javac.tree.JCTree;

import javax.lang.model.element.Element;

public class JAsyncCuContext extends JAsyncContext implements IJAsyncCuContext {

    protected CompilationUnitTree cu;

    public JAsyncCuContext(IJAsyncContext asyncContext, CompilationUnitTree cu) {
        super(asyncContext);
        this.cu = cu;
    }

    @Override
    public CompilationUnitTree getCompilationUnitTree() {
        return cu;
    }

    @Override
    public TreePath getPath(JCTree tree) {
        return trees.getPath(cu, tree);
    }

    @Override
    public JavacScope getScope(JCTree tree) {
        TreePath path = getPath(tree);
        return path != null ? getTrees().getScope(path) : null;
    }

    @Override
    public Element getElement(JCTree tree) {
        TreePath path = getPath(tree);
        return path != null ? trees.getElement(path) : null;
    }
}
