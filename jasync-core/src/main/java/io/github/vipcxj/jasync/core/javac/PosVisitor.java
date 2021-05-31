package io.github.vipcxj.jasync.core.javac;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.Pretty;

import java.io.IOException;
import java.io.Writer;

public class PosVisitor extends Pretty {

    public PosVisitor(Writer writer, boolean b) {
        super(writer, b);
    }

    private void printPosStart(JCTree tree) {
        try {
            print("($" + tree.pos + " ");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printPosEnd() {
        try {
            print("$)");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void visitIf(JCTree.JCIf jcIf) {
        printPosStart(jcIf);
        super.visitIf(jcIf);
        printPosEnd();
    }

    @Override
    public void visitBlock(JCTree.JCBlock jcBlock) {
        printPosStart(jcBlock);
        super.visitBlock(jcBlock);
        printPosEnd();
    }

    @Override
    public void visitTry(JCTree.JCTry jcTry) {
        printPosStart(jcTry);
        super.visitTry(jcTry);
        printPosEnd();
    }

    @Override
    public void visitLambda(JCTree.JCLambda jcLambda) {
        printPosStart(jcLambda);
        super.visitLambda(jcLambda);
        printPosEnd();
    }

    @Override
    public void visitVarDef(JCTree.JCVariableDecl jcVariableDecl) {
        printPosStart(jcVariableDecl);
        super.visitVarDef(jcVariableDecl);
        printPosEnd();
    }

    @Override
    public void visitSelect(JCTree.JCFieldAccess jcFieldAccess) {
        printPosStart(jcFieldAccess);
        super.visitSelect(jcFieldAccess);
        printPosEnd();
    }

    @Override
    public void visitIdent(JCTree.JCIdent jcIdent) {
        printPosStart(jcIdent);
        super.visitIdent(jcIdent);
        printPosEnd();
    }

    @Override
    public void visitApply(JCTree.JCMethodInvocation jcMethodInvocation) {
        printPosStart(jcMethodInvocation);
        super.visitApply(jcMethodInvocation);
        printPosEnd();
    }

    @Override
    public void visitLiteral(JCTree.JCLiteral jcLiteral) {
        printPosStart(jcLiteral);
        super.visitLiteral(jcLiteral);
        printPosEnd();
    }

    @Override
    public void visitReturn(JCTree.JCReturn jcReturn) {
        printPosStart(jcReturn);
        super.visitReturn(jcReturn);
        printPosEnd();
    }
}
