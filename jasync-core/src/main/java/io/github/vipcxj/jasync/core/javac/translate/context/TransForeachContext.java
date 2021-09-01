package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import io.github.vipcxj.jasync.core.javac.Constants;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.context.JAsyncSymbols;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;

public class TransForeachContext extends AbstractTransFrameHolderStatementContext<JCTree.JCEnhancedForLoop> {

    private TransVarDeclContext varContext;
    private TranslateContext<?> exprContext;
    private TransBlockContext bodyContext;

    public TransForeachContext(AnalyzerContext analyzerContext, JCTree.JCEnhancedForLoop tree) {
        super(analyzerContext, tree);
        awaitContainer = tree.expr;
    }

    @Override
    public TransForeachContext enter() {
        super.enter();
        return this;
    }

    @Override
    protected void addNormalChildContext(TranslateContext<?> child) {
        if (varContext == null) {
            checkContextTree(child, tree.var);
            varContext = (TransVarDeclContext) child;
            varContext.setAsyncParam(true);
        } else if (exprContext == null) {
            checkContextTree(child, tree.expr);
            exprContext = child;
        } else if (bodyContext == null) {
            childContextMustBeBlock(child);
            checkContextTree(child, tree.body);
            bodyContext = (TransBlockContext) child;
            bodyContext.setProxyFrame(true);
            bodyContext.setNude(true);
        } else {
            throwIfFull();
        }
    }

    @Override
    protected JCTree buildTreeWithoutThen(boolean replaceSelf) {
        if (hasAwait()) {
            TransMethodContext methodContext = getEnclosingMethodContext();
            IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
            JAsyncSymbols symbols = jasyncContext.getJAsyncSymbols();
            Type type = JavacUtils.getType(jasyncContext, tree.var);
            JCTree.JCExpression methodExpr;
            switch (type.getTag()) {
                case BYTE:
                    methodExpr = symbols.makeJAsyncDoForEachByte();
                    break;
                case CHAR:
                    methodExpr = symbols.makeJAsyncDoForEachChar();
                    break;
                case SHORT:
                    methodExpr = symbols.makeJAsyncDoForEachShort();
                    break;
                case INT:
                    methodExpr = symbols.makeJAsyncDoForEachInt();
                    break;
                case LONG:
                    methodExpr = symbols.makeJAsyncDoForEachLong();
                    break;
                case FLOAT:
                    methodExpr = symbols.makeJAsyncDoForEachFloat();
                    break;
                case DOUBLE:
                    methodExpr = symbols.makeJAsyncDoForEachDouble();
                    break;
                case BOOLEAN:
                    methodExpr = symbols.makeJAsyncDoForEachBoolean();
                    break;
                default:
                    methodExpr = symbols.makeJAsyncDoForEachObject();
            }
            TreeMaker maker = jasyncContext.getTreeMaker();
            int prePos = maker.pos;
            try {
                maker.pos = tree.pos;
                JCTree.JCMethodDecl methodDecl = methodContext.addVoidPromiseFunction(getFrame(), (JCTree.JCBlock) bodyContext.buildTree(false));
                return JavacUtils.makeReturn(jasyncContext, maker.Apply(
                        List.nil(),
                        methodExpr,
                        List.of(
                                (JCTree.JCExpression) exprContext.buildTree(false),
                                methodContext.makeFunctional(getFrame(), Constants.INDY_MAKE_VOID_PROMISE_FUNCTION, methodDecl)
                        )
                ));
            } finally {
                maker.pos = prePos;
            }
        } else {
            tree.var = (JCTree.JCVariableDecl) varContext.buildTree(false);
            tree.expr = (JCTree.JCExpression) exprContext.buildTree(false);
            tree.body = (JCTree.JCStatement) exprContext.buildTree(false);
            return tree;
        }
    }
}
