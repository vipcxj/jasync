package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.model.Frame;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;
import io.github.vipcxj.jasync.core.javac.translate.VarTransHelper;

public class TransIdentContext extends AbstractTransExpressionContext<JCTree.JCIdent> {

    private Frame.CapturedInfo capturedInfo;
    private Frame.DeclInfo declInfo;

    public TransIdentContext(AnalyzerContext analyzerContext, JCTree.JCIdent tree) {
        super(analyzerContext, tree);
    }

    public void setCapturedInfo(Frame.CapturedInfo capturedInfo) {
        this.capturedInfo = capturedInfo;
    }

    public void setDeclInfo(Frame.DeclInfo declInfo) {
        this.declInfo = declInfo;
    }

    private static boolean notInAssignmentLeftArg(TranslateContext<?> context) {
        TranslateContext<?> parent = context.getParent();
        if (parent == null) {
            return true;
        }
        if (parent.getTree() instanceof JCTree.JCStatement) {
            if (parent.getTree() instanceof JCTree.JCVariableDecl) {
                return context.getTree() != ((JCTree.JCVariableDecl) parent.getTree()).getNameExpression();
            } else {
                return true;
            }
        }
        if (parent.getTree() instanceof JCTree.JCAssign) {
            return context.getTree() != ((JCTree.JCAssign) parent.getTree()).getVariable();
        } else {
            return notInAssignmentLeftArg(parent);
        }
    }

    @Override
    public TransIdentContext enter() {
        super.enter();
        return this;
    }

    @Override
    public void complete() {
        if (notInAssignmentLeftArg(this)) {
            analyzerContext.readVar(this);
        }
        super.complete();
    }

    @Override
    public JCTree buildTree(boolean replaceSelf) {
        if (capturedInfo != null || declInfo != null) {
            VarTransHelper helper = new VarTransHelper(capturedInfo, declInfo);
            helper.prepare();
            JCTree.JCExpression newTree;
            if (helper.isRef()) {
                newTree = helper.makeRefGet(analyzerContext.getJasyncContext(), tree);
            } else {
                TreeMaker maker = treeMaker();
                int prePos = maker.pos;
                try {
                    maker.pos = tree.pos;
                    newTree = maker.Ident(helper.getUsedSymbol());
                } finally {
                    maker.pos = prePos;
                }
            }
            if (replaceSelf) {
                replaceBy(newTree);
            }
            return newTree;
        }  else {
            return tree;
        }
    }
}
