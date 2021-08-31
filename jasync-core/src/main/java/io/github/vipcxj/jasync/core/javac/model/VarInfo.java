package io.github.vipcxj.jasync.core.javac.model;

import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;

import javax.lang.model.element.ElementKind;

public class VarInfo {
    private final Symbol.VarSymbol symbol;
    private VarUseState state;
    private boolean initialized;
    private JCTree.JCVariableDecl decl;
    /**
     * only used when state == WRITE and initialized = false.
     */
    private JCTree.JCExpression initializeExpr;
    private String newName;

    public VarInfo(JavacTrees trees, Symbol.VarSymbol symbol) {
        this.symbol = symbol;
        this.state = VarUseState.READ;
        ElementKind kind = symbol.getKind();
        this.initialized = kind == ElementKind.PARAMETER || kind == ElementKind.EXCEPTION_PARAMETER;
        JCTree decl = trees.getTree(symbol);
        if (decl instanceof JCTree.JCVariableDecl) {
            this.decl = (JCTree.JCVariableDecl) decl;
            if (this.decl.init != null) {
                this.initialized = true;
            }
        }
    }

    public VarInfo(JCTree.JCVariableDecl decl) {
        this.symbol = decl.sym;
        this.state = VarUseState.READ;
        ElementKind kind = symbol.getKind();
        this.initialized = decl.init != null || kind == ElementKind.PARAMETER || kind == ElementKind.EXCEPTION_PARAMETER;
        this.decl = decl;
    }

    public Symbol.VarSymbol getSymbol() {
        return symbol;
    }

    public VarUseState getState() {
        return state;
    }

    public void setState(VarUseState state) {
        this.state = state;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public JCTree.JCVariableDecl getDecl() {
        return decl;
    }

    public void setDecl(JCTree.JCVariableDecl decl) {
        this.decl = decl;
    }

    public JCTree.JCExpression getInitializeExpr() {
        return initializeExpr;
    }

    public void setInitializeExpr(JCTree.JCExpression initializeExpr) {
        this.initializeExpr = initializeExpr;
    }

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }
}
