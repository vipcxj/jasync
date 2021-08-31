package io.github.vipcxj.jasync.core.javac.translate;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import io.github.vipcxj.jasync.core.javac.IJAsyncContext;
import io.github.vipcxj.jasync.core.javac.context.JAsyncSymbols;
import io.github.vipcxj.jasync.core.javac.model.Frame;

public class VarTransHelper {
    private final Frame.CapturedInfo capturedInfo;
    private final Frame.DeclInfo declInfo;
    private boolean ref;
    private Symbol.VarSymbol usedSymbol;
    private Symbol.VarSymbol declSymbol;

    public VarTransHelper(Frame.CapturedInfo capturedInfo, Frame.DeclInfo declInfo) {
        this.capturedInfo = capturedInfo;
        this.declInfo = declInfo;
    }

    public void prepare() {
        ref = capturedInfo != null ? capturedInfo.isNotReadOnly() : declInfo.isRefed();
        usedSymbol = capturedInfo != null ? capturedInfo.getUsedSymbol() : declInfo.getUsedSymbol();
        declSymbol = capturedInfo != null ? capturedInfo.getDeclSymbol() : declInfo.getDeclSymbol();
    }

    public boolean isRef() {
        return ref;
    }

    public Symbol.VarSymbol getUsedSymbol() {
        return usedSymbol;
    }

    public JCTree.JCExpression makeRefAssign(IJAsyncContext context, JCTree tree, JCTree.JCExpression expression) {
        if (!ref) {
            throw new IllegalStateException("Must be ref.");
        }
        JAsyncSymbols symbols = context.getJAsyncSymbols();
        TreeMaker maker = context.getTreeMaker();
        int prePos = maker.pos;
        try {
            maker.pos = tree.pos;
            return maker.Parens(maker.Assign(
                    maker.Ident(usedSymbol),
                    symbols.makeRefAssign(declSymbol, tree.getTag(), expression)
            ));
        } finally {
            maker.pos = prePos;
        }
    }

    public JCTree.JCExpression makeRefGet(IJAsyncContext context, JCTree tree) {
        if (!ref) {
            throw new IllegalStateException("Must be ref.");
        }
        JAsyncSymbols symbols = context.getJAsyncSymbols();
        TreeMaker maker = context.getTreeMaker();
        int prePos = maker.pos;
        try {
            maker.pos = tree.pos;
            return maker.Parens(maker.Assign(
                    maker.Ident(usedSymbol),
                    symbols.makeRefGet(declSymbol)
            ));
        } finally {
            maker.pos = prePos;
        }
    }
}
