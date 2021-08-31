package io.github.vipcxj.jasync.core.javac.visitor;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;

public abstract class StatementAnalyzer extends TreeScanner {

    private JCTree.JCStatement statement;

    protected void analyzeVarDef(JCTree.JCVariableDecl tree) {
        super.visitVarDef(tree);
    }

    @Override
    public void visitVarDef(JCTree.JCVariableDecl tree) {
        JCTree.JCStatement pre = this.statement;
        try {
            analyzeVarDef(tree);
        } finally {
            this.statement = pre;
        }
    }

    protected void analyzeExec(JCTree.JCExpressionStatement tree) {
        super.visitExec(tree);
    }

    @Override
    public void visitExec(JCTree.JCExpressionStatement tree) {
        JCTree.JCStatement pre = this.statement;
        try {
            analyzeExec(tree);
        } finally {
            this.statement = pre;
        }
    }

    protected void analyzeAssert(JCTree.JCAssert tree) {
        super.visitAssert(tree);
    }

    @Override
    public void visitAssert(JCTree.JCAssert tree) {
        JCTree.JCStatement pre = this.statement;
        try {
            analyzeAssert(tree);
        } finally {
            this.statement = pre;
        }
    }

    protected void analyzeBreak(JCTree.JCBreak tree) {
        super.visitBreak(tree);
    }

    @Override
    public void visitBreak(JCTree.JCBreak tree) {
        JCTree.JCStatement pre = this.statement;
        try {
            analyzeBreak(tree);
        } finally {
            this.statement = pre;
        }
    }

    protected void analyzeContinue(JCTree.JCContinue tree) {
        super.visitContinue(tree);
    }

    @Override
    public void visitContinue(JCTree.JCContinue tree) {
        JCTree.JCStatement pre = this.statement;
        try {
            analyzeContinue(tree);
        } finally {
            this.statement = pre;
        }
    }

    protected void analyzeSkip(JCTree.JCSkip tree) {
        super.visitSkip(tree);
    }

    @Override
    public void visitSkip(JCTree.JCSkip tree) {
        JCTree.JCStatement pre = this.statement;
        try {
            analyzeSkip(tree);
        } finally {
            this.statement = pre;
        }
    }

    protected void analyzeThrow(JCTree.JCThrow tree) {
        super.visitThrow(tree);
    }

    @Override
    public void visitThrow(JCTree.JCThrow tree) {
        JCTree.JCStatement pre = this.statement;
        try {
            analyzeThrow(tree);
        } finally {
            this.statement = pre;
        }
    }

    protected void analyzeReturn(JCTree.JCReturn tree) {
        super.visitReturn(tree);
    }

    @Override
    public void visitReturn(JCTree.JCReturn tree) {
            JCTree.JCStatement pre = this.statement;
            try {
                analyzeReturn(tree);
            } finally {
                this.statement = pre;
            }
    }

    protected void analyzeSynchronized(JCTree.JCSynchronized tree) {
        super.visitSynchronized(tree);
    }

    @Override
    public void visitSynchronized(JCTree.JCSynchronized tree) {
        JCTree.JCStatement pre = this.statement;
        try {
            analyzeSynchronized(tree);
        } finally {
            this.statement = pre;
        }
    }

    protected void analyzeLabelled(JCTree.JCLabeledStatement tree) {
        super.visitLabelled(tree);
    }

    @Override
    public void visitLabelled(JCTree.JCLabeledStatement tree) {
        JCTree.JCStatement pre = this.statement;
        try {
            analyzeLabelled(tree);
        } finally {
            this.statement = pre;
        }
    }

    protected void analyzeBlock(JCTree.JCBlock tree) {
        super.visitBlock(tree);
    }

    @Override
    public void visitBlock(JCTree.JCBlock tree) {
        JCTree.JCStatement pre = this.statement;
        try {
            analyzeBlock(tree);
        } finally {
            this.statement = pre;
        }
    }

    protected void analyzeIf(JCTree.JCIf tree) {
        super.visitIf(tree);
    }

    @Override
    public void visitIf(JCTree.JCIf tree) {
        JCTree.JCStatement pre = this.statement;
        try {
            analyzeIf(tree);
        } finally {
            this.statement = pre;
        }
    }

    protected void analyzeSwitch(JCTree.JCSwitch tree) {
        super.visitSwitch(tree);
    }

    @Override
    public void visitSwitch(JCTree.JCSwitch tree) {
        JCTree.JCStatement pre = this.statement;
        try {
            analyzeSwitch(tree);
        } finally {
            this.statement = pre;
        }
    }

    protected void analyzeCase(JCTree.JCCase tree) {
        super.visitCase(tree);
    }

    @Override
    public void visitCase(JCTree.JCCase tree) {
        JCTree.JCStatement pre = this.statement;
        try {
            analyzeCase(tree);
        } finally {
            this.statement = pre;
        }
    }

    protected void analyzeTry(JCTree.JCTry tree) {
        super.visitTry(tree);
    }

    @Override
    public void visitTry(JCTree.JCTry tree) {
        JCTree.JCStatement pre = this.statement;
        try {
            analyzeTry(tree);
        } finally {
            this.statement = pre;
        }
    }

    protected void analyzeCatch(JCTree.JCCatch tree) {
        super.visitCatch(tree);
    }

    @Override
    public void visitCatch(JCTree.JCCatch tree) {
        JCTree.JCStatement pre = this.statement;
        try {
            analyzeCatch(tree);
        } finally {
            this.statement = pre;
        }
    }

    protected void analyzeForLoop(JCTree.JCForLoop tree) {
        super.visitForLoop(tree);
    }

    @Override
    public void visitForLoop(JCTree.JCForLoop tree) {
        JCTree.JCStatement pre = this.statement;
        try {
            analyzeForLoop(tree);
        } finally {
            this.statement = pre;
        }
    }

    protected void analyzeForeachLoop(JCTree.JCEnhancedForLoop tree) {
        super.visitForeachLoop(tree);
    }

    @Override
    public void visitForeachLoop(JCTree.JCEnhancedForLoop tree) {
        JCTree.JCStatement pre = this.statement;
        try {
            analyzeForeachLoop(tree);
        } finally {
            this.statement = pre;
        }
    }

    protected void analyzeWhileLoop(JCTree.JCWhileLoop tree) {
        super.visitWhileLoop(tree);
    }

    @Override
    public void visitWhileLoop(JCTree.JCWhileLoop tree) {
        JCTree.JCStatement pre = this.statement;
        try {
            analyzeWhileLoop(tree);
        } finally {
            this.statement = pre;
        }
    }

    protected void analyzeDoLoop(JCTree.JCDoWhileLoop tree) {
        super.visitDoLoop(tree);
    }

    @Override
    public void visitDoLoop(JCTree.JCDoWhileLoop tree) {
        JCTree.JCStatement pre = this.statement;
        try {
            analyzeDoLoop(tree);
        } finally {
            this.statement = pre;
        }
    }
}
