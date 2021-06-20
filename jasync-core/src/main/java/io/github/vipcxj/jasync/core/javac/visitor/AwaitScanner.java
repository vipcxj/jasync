package io.github.vipcxj.jasync.core.javac.visitor;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import io.github.vipcxj.jasync.core.javac.Constants;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public class AwaitScanner extends ShallowTreeBooleanScanner {

    private final IJAsyncInstanceContext context;
    private boolean result;

    public AwaitScanner(IJAsyncInstanceContext context) {
        this.context = context;
        this.result = false;
    }

    @Override
    public Boolean visitMethodInvocation(MethodInvocationTree methodInvocationTree, Void aVoid) {
        if (result) return true;
        Element element = context.getElement((JCTree) methodInvocationTree);
        if (element instanceof ExecutableElement) {
            if (element.getSimpleName().toString().equals("await")) {
                Elements elementUtils = context.getEnvironment().getElementUtils();
                Types typeUtils = context.getEnvironment().getTypeUtils();
                TypeElement promiseElement = elementUtils.getTypeElement(Constants.PROMISE);
                TypeMirror promiseType = promiseElement.asType();
                if (typeUtils.isAssignable(element.getEnclosingElement().asType(), promiseType)) {
                    result = true;
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean checkTree(IJAsyncInstanceContext context, JCTree tree) {
        AwaitScanner scanner = new AwaitScanner(context);
        return Boolean.TRUE.equals(tree.accept(scanner, null));
    }

    public static boolean checkStatements(IJAsyncInstanceContext context, List<JCTree.JCStatement> statements) {
        AwaitScanner scanner = new AwaitScanner(context);
        for (JCTree.JCStatement statement : statements) {
            if (Boolean.TRUE.equals(statement.accept(scanner, null))) {
                return true;
            }
        }
        return false;
    }
}
