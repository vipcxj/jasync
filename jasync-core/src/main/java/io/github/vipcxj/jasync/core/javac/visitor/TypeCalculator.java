package io.github.vipcxj.jasync.core.javac.visitor;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;

public class TypeCalculator extends TreeScanner {

    private Type type;

    public Type getType() {
        return type;
    }

    @Override
    public void scan(JCTree tree) {
        if (tree != null && type == null) {
            if (tree.type != null) {
                this.type = tree.type;
            } else if (tree instanceof JCTree.JCVariableDecl) {
                JCTree.JCVariableDecl decl = (JCTree.JCVariableDecl) tree;
                if (decl.sym != null && decl.sym.type != null) {
                    this.type = decl.sym.type;
                }
            }
        }
    }

    @Override
    public void visitIdent(JCTree.JCIdent tree) {
        scan(tree);
    }

    @Override
    public void visitLiteral(JCTree.JCLiteral tree) {
        scan(tree);
    }

    @Override
    public void visitTypeIdent(JCTree.JCPrimitiveTypeTree tree) {
        scan(tree);
    }
}
