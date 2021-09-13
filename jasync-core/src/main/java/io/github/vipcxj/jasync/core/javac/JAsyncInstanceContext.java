package io.github.vipcxj.jasync.core.javac;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacScope;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Log;
import io.github.vipcxj.jasync.core.javac.model.JAsyncInfo;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

public class JAsyncInstanceContext extends JAsyncContext implements IJAsyncInstanceContext {

    protected final JAsyncInstanceContext parent;
    protected final ExecutableElement methodRoot;
    protected final CompilationUnitTree cu;
    protected final JAsyncInfo info;
    protected int pos;

    public JAsyncInstanceContext(IJAsyncContext asyncContext, ExecutableElement methodRoot) {
        super(asyncContext);
        this.parent = null;
        this.methodRoot = methodRoot;
        JavacTrees javacTrees = getTrees();
        TreePath path = javacTrees.getPath(methodRoot);
        this.cu = path.getCompilationUnit();
        this.pos = calcSafePos((JCTree.JCCompilationUnit) cu);
        AnnotationMirror async = AnnotationUtils.getAnnotationDirectOn(methodRoot, Constants.ASYNC);
        this.info = new JAsyncInfo(this, async);
    }

    public JAsyncInstanceContext(ExecutableElement methodRoot, JAsyncInstanceContext parent) {
        super(parent);
        this.parent = parent;
        this.methodRoot = methodRoot;
        this.cu = null;
        AnnotationMirror async = AnnotationUtils.getAnnotationDirectOn(methodRoot, Constants.ASYNC);
        this.info = new JAsyncInfo(this, async);
    }

    private static int calcSafePos(JCTree.JCCompilationUnit cu) {
        TreeSafePosCalculator calculator = new TreeSafePosCalculator();
        calculator.scan(cu);
        return calculator.getPos() + 1;
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
        return parent != null ? parent.cu : cu;
    }

    @Override
    public TreePath getPath(JCTree tree) {
        return trees.getPath(getCompilationUnitTree(), tree);
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

    @Override
    public TreeMaker safeMaker() {
        TreeMaker maker = getTreeMaker();
        maker.pos = incPos();
        return maker;
    }

    private int incPos() {
        if (parent != null) {
            return parent.incPos();
        }
        return pos++;
    }

    static class TreeSafePosCalculator extends TreeScanner {

        private int pos = Integer.MIN_VALUE;

        @Override
        public void scan(JCTree tree) {
            if (tree != null && tree.pos > pos) {
                pos = tree.pos;
            }
            super.scan(tree);
        }

        public int getPos() {
            return pos;
        }
    }
}
