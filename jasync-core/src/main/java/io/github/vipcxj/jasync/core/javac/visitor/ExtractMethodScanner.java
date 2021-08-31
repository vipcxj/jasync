package io.github.vipcxj.jasync.core.javac.visitor;

import com.sun.source.tree.LineMap;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.List;
import io.github.vipcxj.jasync.core.javac.model.VarKey;

import javax.lang.model.element.ElementKind;
import java.util.HashMap;
import java.util.Map;

public class ExtractMethodScanner extends TreeScanner {

    private final ExtractMethodContext extractMethodContext;

    public ExtractMethodScanner(LineMap lineMap) {
        this.extractMethodContext = new ExtractMethodContext(lineMap);
    }

    public ExtractMethodScanner(ExtractMethodContext extractMethodContext) {
        this.extractMethodContext = extractMethodContext;
    }

    @Override
    public void visitVarDef(JCTree.JCVariableDecl tree) {
        extractMethodContext.localDeclMap.put(new VarKey(tree.sym), tree);
        super.visitVarDef(tree);
    }

    @Override
    public void visitIdent(JCTree.JCIdent tree) {
        if (tree.sym instanceof Symbol.VarSymbol) {
            Symbol.VarSymbol varSymbol = (Symbol.VarSymbol) tree.sym;
            if (!extractMethodContext.localDeclMap.containsKey(new VarKey(varSymbol))) {
                if (varSymbol.getKind() == ElementKind.LOCAL_VARIABLE
                        || varSymbol.getKind() == ElementKind.PARAMETER
                        || varSymbol.getKind() == ElementKind.EXCEPTION_PARAMETER
                        || varSymbol.getKind() == ElementKind.RESOURCE_VARIABLE
                ) {
                    extractMethodContext.addCapturedVar(tree);
                }
            }
        }
    }

    public static ExtractMethodContext scanStatements(LineMap lineMap, List<JCTree.JCStatement> statements) {
        ExtractMethodContext extractMethodContext = new ExtractMethodContext(lineMap);
        for (JCTree.JCStatement statement : statements) {
            statement.accept(new ExtractMethodScanner(extractMethodContext));
        }
        return extractMethodContext;
    }

    public static class ExtractMethodContext {
        private final LineMap lineMap;
        private final Map<VarKey, JCTree.JCVariableDecl> localDeclMap;
        private final Map<VarKey, List<JCTree.JCIdent>>capturedVarMap;

        public ExtractMethodContext(LineMap lineMap) {
            this.lineMap = lineMap;
            this.localDeclMap = new HashMap<>();
            this.capturedVarMap = new HashMap<>();
        }

        private void addCapturedVar(JCTree.JCIdent ident) {
            Symbol.VarSymbol sym = (Symbol.VarSymbol) ident.sym;
            VarKey key = new VarKey(sym);
            List<JCTree.JCIdent> idents = capturedVarMap.get(key);
            if (idents == null) {
                idents = List.of(ident);
            } else {
                idents = idents.prepend(ident);
            }
            capturedVarMap.put(key, idents);
        }

        public void printCaptured() {
            for (Map.Entry<VarKey, List<JCTree.JCIdent>> entry : capturedVarMap.entrySet()) {
                System.out.print("Captured " + entry.getKey().getName() + " at ");
                int i = 0;
                for (JCTree.JCIdent ident : entry.getValue()) {
                    System.out.print("[" + lineMap.getLineNumber(ident.pos) + ", " + lineMap.getColumnNumber(ident.pos) + "]");
                    if (++i != entry.getValue().size()) {
                        System.out.print(", ");
                    }
                }
                System.out.println(".");
            }
        }
    }
}
