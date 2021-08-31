package io.github.vipcxj.jasync.core.javac.model;

import com.sun.tools.javac.code.Symbol;

import java.util.Objects;

public class VarKey {

    private final Symbol symbol;
    private final String name;
    private final int pos;

    public VarKey(Symbol symbol) {
        this.symbol = symbol;
        this.name = symbol.getSimpleName().toString();
        this.pos = getPos(symbol);
    }

    private static int getPos(Symbol symbol) {
        if (symbol instanceof Symbol.VarSymbol) {
            return ((Symbol.VarSymbol) symbol).pos;
        } else if (symbol instanceof Symbol.DelegatedSymbol) {
            Symbol underlyingSymbol = ((Symbol.DelegatedSymbol<?>) symbol).getUnderlyingSymbol();
            return getPos(underlyingSymbol);
        } else {
            return -1;
        }
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public int getPos() {
        return pos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VarKey varKey = (VarKey) o;
        return pos == varKey.pos &&
                Objects.equals(name, varKey.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, pos);
    }

    @Override
    public String toString() {
        return "VarKey{" +
                "name='" + name + '\'' +
                ", pos=" + pos +
                '}';
    }
}
