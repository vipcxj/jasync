package io.github.vipcxj.jasync.core.javac.visitor;

import com.sun.tools.javac.api.JavacScope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.List;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.model.VarInfo;
import io.github.vipcxj.jasync.core.javac.model.VarKey;
import io.github.vipcxj.jasync.core.javac.model.VarUseState;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.github.vipcxj.jasync.core.javac.JavacUtils.equalSymbol;

public class ScopeVarScanner extends TreeScanner {

    private final IJAsyncInstanceContext context;
    private final JavacScope scope;
    private final Map<VarKey, VarInfo> varData;
    private boolean scanReshaped = false;

    public ScopeVarScanner(IJAsyncInstanceContext context, JavacScope scope) {
        this.context = context;
        this.scope = scope;
        this.varData = new HashMap<>();
    }

    public Map<VarKey, VarInfo> getVarData() {
        return varData;
    }

    private Symbol.VarSymbol getVarSymbol(JCTree tree) {
        Element element = context.getElement(tree);
        if (element instanceof Symbol.VarSymbol) {
            Symbol.VarSymbol varSymbol = (Symbol.VarSymbol) element;
            for (Element localElement : scope.getLocalElements()) {
                if (localElement instanceof Symbol.VarSymbol) {
                    Symbol.VarSymbol localVarSymbol = (Symbol.VarSymbol) localElement;
                    if (equalSymbol(varSymbol, localVarSymbol)) {
                        return varSymbol;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void visitVarDef(JCTree.JCVariableDecl jcVariableDecl) {
        if (!scanReshaped) {
            Symbol.VarSymbol varSymbol = getVarSymbol(jcVariableDecl);
            if (varSymbol != null) {
                VarKey varKey = new VarKey(varSymbol);
                VarInfo varInfo = varData.get(varKey);
                if (varInfo == null) {
                    varInfo = new VarInfo(context.getTrees(), varSymbol);
                    varInfo.setState(VarUseState.READ);
                }
                if (varSymbol.getKind() == ElementKind.PARAMETER || varSymbol.getKind() == ElementKind.EXCEPTION_PARAMETER) {
                    varInfo.setInitialized(true);
                } else {
                    varInfo.setInitialized(jcVariableDecl.init != null);
                }
                varData.put(varKey, varInfo);
            }
            scan(jcVariableDecl.init);
        } else {
            super.visitVarDef(jcVariableDecl);
        }
    }

    @Override
    public void visitIdent(JCTree.JCIdent jcIdent) {
        Symbol.VarSymbol varSymbol = getVarSymbol(jcIdent);
        if (varSymbol != null) {
            VarKey varKey = new VarKey(varSymbol);
            VarInfo varInfo = varData.get(varKey);
            if (varInfo == null) {
                varInfo = new VarInfo(context.getTrees(), varSymbol);
                varInfo.setState(VarUseState.READ);
            }
            if (varInfo.getState() == VarUseState.READ && !varInfo.isInitialized()) {
                varInfo.setState(VarUseState.ERROR);
            }
            varData.putIfAbsent(varKey, varInfo);
        }
        super.visitIdent(jcIdent);
    }

    private void visitAssignLeft(JCTree.JCExpression source, JCTree.JCExpression expression) {
        Symbol.VarSymbol varSymbol = getVarSymbol(expression);
        if (varSymbol != null) {
            VarKey key = new VarKey(varSymbol);
            VarInfo info = varData.get(key);
            if (info == null) {
                info = new VarInfo(context.getTrees(), varSymbol);
            }
            if (info.getState() != VarUseState.ERROR) {
                if (scanReshaped) {
                    info.setState(VarUseState.WRITE);
                    if (!info.isInitialized()) {
                        info.setInitializeExpr(source);
                    }
                } else {
                    info.setInitialized(true);
                    if (info.getState() != VarUseState.WRITE) {
                        info.setState(VarUseState.WRITE_BEFORE);
                    }
                }
            }
            varData.put(key, info);
        }
    }

    @Override
    public void visitAssign(JCTree.JCAssign jcAssign) {
        visitAssignLeft(jcAssign, jcAssign.lhs);
        scan(jcAssign.rhs);
    }

    @Override
    public void visitAssignop(JCTree.JCAssignOp jcAssignOp) {
        visitAssignLeft(jcAssignOp, jcAssignOp.lhs);
        super.visitAssignop(jcAssignOp);
    }

    @Override
    public void visitUnary(JCTree.JCUnary jcUnary) {
        if (jcUnary.getTag().isIncOrDecUnaryOp()) {
            visitAssignLeft(jcUnary, jcUnary.arg);
        }
        super.visitUnary(jcUnary);
    }

    public static Map<VarKey, VarInfo> scanVar(IJAsyncInstanceContext context, List<JCTree.JCStatement> preScopeStats, JCTree.JCExpression reshapedExpr) {
        if (preScopeStats.isEmpty()) {
            return Collections.emptyMap();
        }
        JavacScope scope = context.getScope(preScopeStats.last());
        ScopeVarScanner scanner = new ScopeVarScanner(context, scope);
        scanner.scanReshaped = false;
        for (JCTree.JCStatement statement : preScopeStats) {
            statement.accept(scanner);
        }
        scanner.scanReshaped = true;
        reshapedExpr.accept(scanner);
        return scanner.getVarData();
    }
}
