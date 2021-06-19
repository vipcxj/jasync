package io.github.vipcxj.jasync.core.javac;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacScope;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Log;

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
        Log.DiscardDiagnosticHandler handler = new Log.DiscardDiagnosticHandler(getLog());
        try {
            return path != null ? getTrees().getScope(path) : null;
        } finally {
            getLog().popDiagnosticHandler(handler);
        }
    }

    @Override
    public Element getElement(JCTree tree) {
        TreePath path = getPath(tree);
        return path != null ? trees.getElement(path) : null;
    }
}
