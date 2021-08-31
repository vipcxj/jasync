package io.github.vipcxj.jasync.core.javac.visitor;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;
import io.github.vipcxj.jasync.core.javac.model.CaseVarInfo;

import javax.lang.model.element.Element;
import java.util.*;

public class SwitchScanner extends TreeScanner {

    private final IJAsyncInstanceContext context;
    private final Map<String, CaseVarInfo> varData;
    private JCTree.JCCase currentCase;
    private final Stack<JCTree.JCBlock> blocks;

    public SwitchScanner(IJAsyncInstanceContext context) {
        this.context = context;
        this.varData = new HashMap<>();
        this.blocks = new Stack<>();
    }

    public Map<JCTree.JCCase, List<Symbol.VarSymbol>> collectNeedRedeclareVars() {
        Map<JCTree.JCCase, List<Symbol.VarSymbol>> varMap = new HashMap<>();
        for (CaseVarInfo info : varData.values()) {
            for (JCTree.JCCase aCase : info.needRedeclareCases()) {
                List<Symbol.VarSymbol> symbols = varMap.computeIfAbsent(aCase, k -> new ArrayList<>());
                symbols.add(info.getSymbol());
            }
        }
        return varMap;
    }

    private Symbol.VarSymbol getSymbol(JCTree tree) {
        Element element = context.getElement(tree);
        if (element instanceof Symbol.VarSymbol) {
            return (Symbol.VarSymbol) element;
        }
        return null;
    }

    @Override
    public void visitLambda(JCTree.JCLambda jcLambda) { }

    @Override
    public void visitMethodDef(JCTree.JCMethodDecl jcMethodDecl) { }

    @Override
    public void visitClassDef(JCTree.JCClassDecl jcClassDecl) { }

    @Override
    public void visitBlock(JCTree.JCBlock jcBlock) {
        this.blocks.push(jcBlock);
        if (jcBlock.stats != null) {
            for (JCTree.JCStatement stat : jcBlock.stats) {
                scan(stat);
            }
        }
        this.blocks.pop();
    }

    @Override
    public void visitVarDef(JCTree.JCVariableDecl jcVariableDecl) {
        this.scan(jcVariableDecl.init);
        if (!this.blocks.isEmpty()) {
            return;
        }
        Element element = context.getElement(jcVariableDecl);
        if (element instanceof Symbol.VarSymbol) {
            Symbol.VarSymbol symbol = (Symbol.VarSymbol) element;
            String name = symbol.name.toString();
            CaseVarInfo info = varData.get(name);
            if (info != null) {
                info.define(currentCase);
            } else {
                varData.put(name, new CaseVarInfo(symbol, currentCase, jcVariableDecl.init != null));
            }
        }
    }

    @Override
    public void visitIdent(JCTree.JCIdent jcIdent) {
        CaseVarInfo info = varData.get(jcIdent.name.toString());
        if (info != null) {
            Symbol.VarSymbol symbol = getSymbol(jcIdent);
            if (JavacUtils.equalSymbol(context, info.getSymbol(), symbol)) {
                info.read(currentCase);
            }
        }
    }

    private void scanAssign(JCTree.JCIdent jcIdent) {
        CaseVarInfo info = varData.get(jcIdent.toString());
        if (info != null) {
            Symbol.VarSymbol symbol = getSymbol(jcIdent);
            if (JavacUtils.equalSymbol(context, info.getSymbol(), symbol)) {
                info.write(currentCase);
            }
        }
    }

    @Override
    public void visitAssign(JCTree.JCAssign jcAssign) {
        scan(jcAssign.rhs);
        if (jcAssign.lhs instanceof JCTree.JCIdent) {
            JCTree.JCIdent lhs = (JCTree.JCIdent) jcAssign.lhs;
            scanAssign(lhs);
        }
    }

    @Override
    public void visitAssignop(JCTree.JCAssignOp jcAssignOp) {
        scan(jcAssignOp.rhs);
        if (jcAssignOp.lhs instanceof JCTree.JCIdent) {
            JCTree.JCIdent lhs = (JCTree.JCIdent) jcAssignOp.lhs;
            scanAssign(lhs);
        }
    }

    @Override
    public void visitUnary(JCTree.JCUnary jcUnary) {
        if (jcUnary.getTag().isIncOrDecUnaryOp()) {
            if (jcUnary.arg instanceof JCTree.JCIdent) {
                JCTree.JCIdent arg = (JCTree.JCIdent) jcUnary.arg;
                scanAssign(arg);
            }
        }
    }

    @Override
    public void visitSwitch(JCTree.JCSwitch jcSwitch) {
        for (JCTree.JCCase aCase : jcSwitch.cases) {
            currentCase = aCase;
            for (JCTree.JCStatement stat : aCase.stats) {
                scan(stat);
            }
            currentCase = null;
        }
    }
}
