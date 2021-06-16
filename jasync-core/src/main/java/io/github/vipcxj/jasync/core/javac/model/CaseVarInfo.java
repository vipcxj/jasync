package io.github.vipcxj.jasync.core.javac.model;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CaseVarInfo {
    private final Symbol.VarSymbol symbol;
    private final JCTree.JCCase jcCase;
    private final Map<JCTree.JCCase, VarState> varStateMap;

    public CaseVarInfo(Symbol.VarSymbol symbol, JCTree.JCCase jcCase, boolean initialized) {
        this.symbol = symbol;
        this.jcCase = jcCase;
        this.varStateMap = new HashMap<>();
        VarState state = new VarState();
        state.setInitialized(initialized);
        varStateMap.put(jcCase, state);
    }

    public Symbol.VarSymbol getSymbol() {
        return symbol;
    }

    public JCTree.JCCase getCase() {
        return jcCase;
    }

    private VarState getState(JCTree.JCCase jcCase) {
        VarState state = varStateMap.get(jcCase);
        if (state == null) {
            state = new VarState();
            state.setInitialized(false);
            varStateMap.put(jcCase, state);
        }
        return state;
    }

    public void define(JCTree.JCCase jcCase) {
        VarState state = getState(jcCase);
        state.setRedefined(true);
    }

    public void read(JCTree.JCCase jcCase) {
        VarState state = getState(jcCase);
        state.setRead(true);
        if (!state.isInitialized()) {
            state.setReadBeforeInitialized(true);
        }
    }

    public void write(JCTree.JCCase jcCase) {
        VarState state = getState(jcCase);
        state.setWrite(true);
        if (!state.isInitialized()) {
            state.setInitialized(true);
        }
    }

    public List<JCTree.JCCase>needRedeclareCases() {
        List<JCTree.JCCase> cases = new ArrayList<>();
        for (Map.Entry<JCTree.JCCase, VarState> entry : varStateMap.entrySet()) {
            VarState state = entry.getValue();
            if (entry.getKey() != jcCase && state != null) {
                cases.add(entry.getKey());
            }
        }
        return cases;
    }
}
