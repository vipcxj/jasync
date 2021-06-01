package io.github.vipcxj.jasync.core.javac;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.tree.JCTree;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public class AwaitScanner extends ShallowTreeBooleanScanner {

    private final IJAsyncCuContext context;
    private boolean result;

    public AwaitScanner(IJAsyncCuContext context) {
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
}
