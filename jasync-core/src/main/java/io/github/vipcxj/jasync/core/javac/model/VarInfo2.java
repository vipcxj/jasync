package io.github.vipcxj.jasync.core.javac.model;

import com.sun.tools.javac.code.Symbol;

public class VarInfo2 {

    private final Symbol symbol;
    private boolean initialized;
    private boolean readOnly;
    private boolean captured;


    public VarInfo2(Symbol symbol) {
        this.symbol = symbol;
        this.readOnly = true;
        this.initialized = false;
        this.captured = false;
    }

    public VarInfo2 copy() {
        return new VarInfo2(symbol)
                .setInitialized(initialized)
                .setReadOnly(readOnly);
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public VarInfo2 setInitialized(boolean initialized) {
        this.initialized = initialized;
        return this;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public VarInfo2 setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    public boolean isCaptured() {
        return captured;
    }

    public VarInfo2 setCaptured(boolean captured) {
        this.captured = captured;
        return this;
    }
}
