package io.github.vipcxj.jasync.core.javac.visitor;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreeScanner;

public class ShallowTreeBooleanScanner extends TreeScanner<Boolean, Void> {

    @Override
    public Boolean reduce(Boolean r1, Boolean r2) {
        return Boolean.TRUE.equals(r1) || Boolean.TRUE.equals(r2);
    }

    @Override
    public Boolean visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitClass(ClassTree classTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitMethod(MethodTree methodTree, Void aVoid) {
        return false;
    }
}
