package io.github.vipcxj.jasync.core.javac;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;

public class ShallowTranslator extends TreeTranslator {

    @Override
    public void visitLambda(JCTree.JCLambda jcLambda) {
        result = jcLambda;
    }

    @Override
    public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
        result = jcClassDecl;
    }

    @Override
    public void visitMethodDef(JCTree.JCMethodDecl jcMethodDecl) {
        result = jcMethodDecl;
    }
}
