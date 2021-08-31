package io.github.vipcxj.jasync.core.javac.translator;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;

public class CaseVarCleanSymTranslator extends TreeTranslator {

    private final IJAsyncInstanceContext context;
    private final Symbol.VarSymbol symbol;

    public CaseVarCleanSymTranslator(IJAsyncInstanceContext context, Symbol.VarSymbol symbol) {
        this.context = context;
        this.symbol = symbol;
    }

    @Override
    public void visitIdent(JCTree.JCIdent tree) {
        if (tree.sym instanceof Symbol.VarSymbol && JavacUtils.equalSymbol(context, tree.sym, symbol)) {
            tree.sym = null;
        }
        result = tree;
    }
}
