package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.model.Frame;
import io.github.vipcxj.jasync.core.javac.translate.TransPlaceHolderContext;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;

public class TransTreePlaceHolderContext<T extends JCTree>
        extends AbstractTranslateContext<T>
        implements TransPlaceHolderContext<T> {

    private Frame.PlaceHolderInfo declPlaceHolderInfo;
    private Frame.PlaceHolderInfo capturedPlaceHolderInfo;

    public TransTreePlaceHolderContext(AnalyzerContext analyzerContext, T tree) {
        super(analyzerContext, tree);
    }

    public static Frame.PlaceHolderInfo getDeclPlaceHolder(JCTree tree, Frame frame) {
        while (frame != null) {
            Frame.PlaceHolderInfo info = frame.getDeclaredPlaceHolders().get(tree);
            if (info != null) {
                return info;
            }
            frame = frame.getParent();
        }
        return null;
    }

    @Override
    public Frame.PlaceHolderInfo getDeclPlaceHolder() {
        if (declPlaceHolderInfo == null) {
            declPlaceHolderInfo = getDeclPlaceHolder(tree, getFrame());
        }
        return declPlaceHolderInfo;
    }

    @Override
    public void setCapturedPlaceHolder(Frame.PlaceHolderInfo placeHolder) {
        this.capturedPlaceHolderInfo = placeHolder;
    }

    @SuppressWarnings("unchecked")
    public static <T extends JCTree> TransPlaceHolderContext<T> createContext(AnalyzerContext context, T tree) {
        if (tree instanceof JCTree.JCExpression) {
            return (TransPlaceHolderContext<T>) new TransExpressionPlaceHolderContext<>(context, (JCTree.JCExpression) tree);
        } else if (tree instanceof JCTree.JCStatement) {
            return (TransPlaceHolderContext<T>) new TransStatementPlaceHolderContext<>(context, (JCTree.JCStatement) tree);
        } else {
            return new TransTreePlaceHolderContext<>(context, tree);
        }
    }

    private static JCTree.JCIdent buildTree(TranslateContext<?> context, Frame.PlaceHolderInfo placeHolder, boolean replaceSelf) {
        IJAsyncInstanceContext jasyncContext = context.getContext().getJasyncContext();
        JCTree.JCIdent newTree = JavacUtils.wrapPos(
                jasyncContext,
                jasyncContext.getTreeMaker().Ident(placeHolder.getSymbol()),
                context.getTree()
        );
        if (replaceSelf) {
            context.replaceBy(newTree);
        }
        return newTree;
    }

    @Override
    public void complete() {
        getContext().readPlaceHolder(this);
        super.complete();
    }

    @Override
    public JCTree.JCIdent buildTreeWithoutThen(boolean replaceSelf) {
        return buildTree(this, capturedPlaceHolderInfo != null ? capturedPlaceHolderInfo : declPlaceHolderInfo, replaceSelf);
    }

    public static class TransExpressionPlaceHolderContext<T extends JCTree.JCExpression>
            extends AbstractTransExpressionContext<T>
            implements TransPlaceHolderContext<T> {

        private Frame.PlaceHolderInfo declPlaceHolderInfo;
        private Frame.PlaceHolderInfo capturedPlaceHolderInfo;

        public TransExpressionPlaceHolderContext(AnalyzerContext analyzerContext, T tree) {
            super(analyzerContext, tree);
        }

        @Override
        public Frame.PlaceHolderInfo getDeclPlaceHolder() {
            if (declPlaceHolderInfo == null) {
                declPlaceHolderInfo = TransTreePlaceHolderContext.getDeclPlaceHolder(tree, getFrame());
            }
            return declPlaceHolderInfo;
        }

        @Override
        public void setCapturedPlaceHolder(Frame.PlaceHolderInfo placeHolder) {
            this.capturedPlaceHolderInfo = placeHolder;
        }

        @Override
        public JCTree.JCIdent buildTreeWithoutThen(boolean replaceSelf) {
            return TransTreePlaceHolderContext.buildTree(this, capturedPlaceHolderInfo != null ? capturedPlaceHolderInfo : declPlaceHolderInfo, replaceSelf);
        }

        @Override
        public void complete() {
            getContext().readPlaceHolder(this);
            super.complete();
        }
    }

    public static class TransStatementPlaceHolderContext<T extends JCTree.JCStatement>
            extends AbstractTransStatementContext<T>
            implements TransPlaceHolderContext<T> {

        private Frame.PlaceHolderInfo declPlaceHolderInfo;
        private Frame.PlaceHolderInfo capturedPlaceHolderInfo;

        public TransStatementPlaceHolderContext(AnalyzerContext analyzerContext, T tree) {
            super(analyzerContext, tree);
        }

        @Override
        public Frame.PlaceHolderInfo getDeclPlaceHolder() {
            if (declPlaceHolderInfo == null) {
                declPlaceHolderInfo = TransTreePlaceHolderContext.getDeclPlaceHolder(tree, getFrame());
            }
            return declPlaceHolderInfo;
        }

        @Override
        public void setCapturedPlaceHolder(Frame.PlaceHolderInfo placeHolder) {
            this.capturedPlaceHolderInfo = placeHolder;
        }

        @Override
        public JCTree.JCIdent buildTreeWithoutThen(boolean replaceSelf) {
            return TransTreePlaceHolderContext.buildTree(this, capturedPlaceHolderInfo != null ? capturedPlaceHolderInfo : declPlaceHolderInfo, replaceSelf);
        }

        @Override
        public void complete() {
            getContext().readPlaceHolder(this);
            super.complete();
        }

    }
}
