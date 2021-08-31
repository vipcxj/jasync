package io.github.vipcxj.jasync.core.javac.visitor;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeScanner;
import io.github.vipcxj.jasync.core.javac.IJAsyncContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;

public class SymbolReplacer extends TreeScanner {

    private final IJAsyncContext context;
    private final Symbol target;
    private final Symbol replaced;
    private int num;

    public SymbolReplacer(IJAsyncContext context, Symbol target, Symbol replaced) {
        this.context = context;
        this.target = target;
        this.replaced = replaced;
        this.num = 0;
    }

    @Override
    public void scan(JCTree tree) {
        super.scan(tree);
        if (tree != null) {
            Symbol symbol = TreeInfo.symbol(tree);
            if (symbol != null && JavacUtils.equalSymbol(context, symbol, target)) {
                TreeInfo.setSymbol(tree, replaced);
                ++num;
            }
        }
    }

    public boolean happened() {
        return num > 0;
    }
}
