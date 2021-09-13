package io.github.vipcxj.jasync.core.javac.visitor;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.List;
import io.github.vipcxj.jasync.core.javac.AnnotationUtils;
import io.github.vipcxj.jasync.core.javac.Constants;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;
import io.github.vipcxj.jasync.core.javac.translate.context.TransBlockContext;
import io.github.vipcxj.jasync.core.javac.translate.context.TransMethodContext;
import io.github.vipcxj.jasync.core.javac.translate.context.TransNullContext;
import io.github.vipcxj.jasync.core.javac.translate.context.TransTreePlaceHolderContext;
import io.github.vipcxj.jasync.core.javac.translate.factories.TranslateContextFactories;
import io.github.vipcxj.jasync.core.javac.translate.spi.TranslateContextFactory;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

public class JAsyncAnalyzer extends TreeScanner {

    private final AnalyzerContext context;
    private TranslateContext<?> result;

    public JAsyncAnalyzer(IJAsyncInstanceContext context) {
        this.context = new AnalyzerContext(context);
    }

    public JAsyncAnalyzer(AnalyzerContext context) {
        this.context = context;
    }

    private void scan(JCTree tree, boolean forceAwait) {
        if (tree == null) {
            new TransNullContext(context).enter().exit();
        } else {
            if (tree instanceof JCTree.JCMethodDecl) {
                Element element = context.getJasyncContext().getElement(tree);
                AnnotationMirror async = AnnotationUtils.getAnnotationDirectOn(element, Constants.ASYNC);
                if (async != null) {
                    JavacUtils.processAsyncMethod(context.getJasyncContext(), element);
                    return;
                }
            }
            if (context.shouldBeReplaced(tree)) {
                TransTreePlaceHolderContext.createContext(context, tree).enter().exit();
            } else {
                TranslateContextFactory factory = TranslateContextFactories.INSTANCE.getFactory(tree);
                TranslateContext<?> translateContext = factory.create(context, tree);
                if (forceAwait) {
                    translateContext.setAwaitContainer(tree);
                }
                translateContext.enter();
                try {
                    super.scan(tree);
                } finally {
                    result = translateContext;
                    translateContext.exit();
                }
            }
        }
    }

    @Override
    public void scan(JCTree tree) {
        scan(tree, false);
    }

    public void scanMethod(JCTree.JCMethodDecl methodDecl) {
        TransMethodContext methodContext = new TransMethodContext(context, methodDecl);
        methodContext.enter();
        try {
            super.scan(methodDecl);
        } finally {
            result = methodContext;
            methodContext.exit();
        }
    }

    private void wrapScan(JCTree tree, boolean forceAwait) {
        if (tree != null) {
            if (!(tree instanceof JCTree.JCBlock)) {
                TransBlockContext blockContext = new TransBlockContext(context, null).enter();
                try {
                    scan(tree, forceAwait);
                } finally {
                    blockContext.exit();
                }
            } else {
                scan(tree, forceAwait);
            }
        }
    }

    private void wrapScan(JCTree tree) {
        wrapScan(tree, false);
    }

    private <T extends JCTree> void wrapScan(List<T> trees) {
        if (trees != null && !trees.isEmpty()) {
            TransBlockContext blockContext = new TransBlockContext(context, null).enter();
            try {
                scan(trees);
            } finally {
                blockContext.exit();
            }
        }
    }

    @Override
    public void visitWhileLoop(JCTree.JCWhileLoop tree) {
        wrapScan(tree.cond, true);
        wrapScan(tree.body);
    }

    @Override
    public void visitDoLoop(JCTree.JCDoWhileLoop tree) {
        wrapScan(tree.body);
        wrapScan(tree.cond, true);
    }

    @Override
    public void visitForLoop(JCTree.JCForLoop tree) {
        wrapScan(tree.init);
        wrapScan(tree.cond, true);
        wrapScan(tree.step);
        wrapScan(tree.body);
    }

    @Override
    public void visitLambda(JCTree.JCLambda tree) { }

    @Override
    public void visitClassDef(JCTree.JCClassDecl tree) {
        super.scan(tree.defs);
    }

    public static TransMethodContext scan(IJAsyncInstanceContext context, JCTree.JCMethodDecl methodDecl) {
        JAsyncAnalyzer analyzer = new JAsyncAnalyzer(context);
        analyzer.scanMethod(methodDecl);
        return (TransMethodContext) analyzer.result;
    }
}
