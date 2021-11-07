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
import io.github.vipcxj.jasync.core.javac.model.Frame;
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
    public Frame getFrame() {
        if (proxyFrame) {
            return super.getFrame();
        } else {
            // The exprContext may request frame, but is null because exprContext is out of this frame.
            if (frame != null) {
                return frame;
            } else {
                return getParent() != null ? getParent().getFrame() : null;
            }
        }
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
            String methodType;
            switch (type.getTag()) {
                case BYTE:
                    methodExpr = symbols.makeJAsyncDoForEachByte();
                    methodType = Constants.INDY_MAKE_BYTE_VOID_PROMISE_FUNCTION;
                    break;
                case CHAR:
                    methodExpr = symbols.makeJAsyncDoForEachChar();
                    methodType = Constants.INDY_MAKE_CHAR_VOID_PROMISE_FUNCTION;
                    break;
                case SHORT:
                    methodExpr = symbols.makeJAsyncDoForEachShort();
                    methodType = Constants.INDY_MAKE_SHORT_VOID_PROMISE_FUNCTION;
                    break;
                case INT:
                    methodExpr = symbols.makeJAsyncDoForEachInt();
                    methodType = Constants.INDY_MAKE_INT_VOID_PROMISE_FUNCTION;
                    break;
                case LONG:
                    methodExpr = symbols.makeJAsyncDoForEachLong();
                    methodType = Constants.INDY_MAKE_LONG_VOID_PROMISE_FUNCTION;
                    break;
                case FLOAT:
                    methodExpr = symbols.makeJAsyncDoForEachFloat();
                    methodType = Constants.INDY_MAKE_FLOAT_VOID_PROMISE_FUNCTION;
                    break;
                case DOUBLE:
                    methodExpr = symbols.makeJAsyncDoForEachDouble();
                    methodType = Constants.INDY_MAKE_DOUBLE_VOID_PROMISE_FUNCTION;
                    break;
                case BOOLEAN:
                    methodExpr = symbols.makeJAsyncDoForEachBoolean();
                    methodType = Constants.INDY_MAKE_BOOLEAN_VOID_PROMISE_FUNCTION;
                    break;
                default:
                    methodExpr = symbols.makeJAsyncDoForEachObject();
                    methodType = Constants.INDY_MAKE_VOID_PROMISE_FUNCTION;
            }
            TreeMaker maker = jasyncContext.getTreeMaker();
            int prePos = maker.pos;
            try {
                maker.pos = tree.pos;
                JCTree.JCMethodDecl methodDecl = methodContext.addVoidPromiseFunction(bodyContext);
                return JavacUtils.makeReturn(jasyncContext, maker.Apply(
                        List.nil(),
                        methodExpr,
                        List.of(
                                (JCTree.JCExpression) exprContext.buildTree(false),
                                methodContext.makeFunctional(bodyContext.getFrame(), methodType, methodDecl),
                                makeLabelArg()
                        )
                ));
            } finally {
                maker.pos = prePos;
            }
        } else {
            tree.var = (JCTree.JCVariableDecl) varContext.buildTree(false);
            tree.expr = (JCTree.JCExpression) exprContext.buildTree(false);
            tree.body = (JCTree.JCStatement) bodyContext.buildTree(false);
            return tree;
        }
    }

    @Override
    public void complete() {
        if (!proxyFrame) {
            // exprContext should be out of the frame.
            if (exprContext != null) {
                exprContext.complete();
            }
            Frame preFrame = analyzerContext.enter(this);
            frame = analyzerContext.currentFrame();
            frame.markOrder();
            try {
                if (getParent().getThen() == this && getParent() instanceof TransAwaitContext) {
                    TransAwaitContext awaitContext = (TransAwaitContext) getParent();
                    analyzerContext.addPlaceHolder(awaitContext.getTree(), true);
                }
                if (varContext != null) {
                    varContext.complete();
                }
                if (bodyContext != null) {
                    bodyContext.complete();
                }
            } finally {
                analyzerContext.exitTo(preFrame);
            }
            if (thenContext != null) {
                thenContext.complete();
            }
        } else {
            super.complete();
        }
    }
}
