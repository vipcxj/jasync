package io.github.vipcxj.jasync.core.javac.translate;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.model.Frame;

public interface TransWriteExprContext<T extends JCTree.JCExpression> extends TransExpressionContext<T> {
    JCTree.JCExpression getVariableTree();
    Symbol getSymbol();
    void setCapturedInfo(Frame.CapturedInfo capturedInfo);
    void setDeclInfo(Frame.DeclInfo declInfo);
}
