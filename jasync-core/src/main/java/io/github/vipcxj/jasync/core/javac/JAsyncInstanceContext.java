package io.github.vipcxj.jasync.core.javac;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacScope;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Log;
import io.github.vipcxj.jasync.core.javac.model.JAsyncInfo;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

public class JAsyncInstanceContext extends JAsyncContext implements IJAsyncInstanceContext {

    protected ExecutableElement methodRoot;
    protected CompilationUnitTree cu;
    protected JAsyncInfo info;

    public JAsyncInstanceContext(IJAsyncContext asyncContext, ExecutableElement methodRoot) {
        super(asyncContext);
        this.methodRoot = methodRoot;
        JavacTrees javacTrees = getTrees();
        TreePath path = javacTrees.getPath(methodRoot);
        this.cu = path.getCompilationUnit();
        AnnotationMirror async = AnnotationUtils.getAnnotationDirectOn(methodRoot, Constants.ASYNC);
        this.info = new JAsyncInfo(this, async);
    }

    @Override
    public ExecutableElement getMethodRoot() {
        return methodRoot;
    }

    @Override
    public JAsyncInfo getInfo() {
        return info;
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
