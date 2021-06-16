package io.github.vipcxj.jasync.core.javac.model;

import com.sun.tools.javac.code.Symbol;

import java.util.Objects;

public class VarKey {

    private final String name;
    private final int pos;

    public VarKey(Symbol.VarSymbol symbol) {
        this(symbol.getSimpleName().toString(), symbol.pos);
    }

    public VarKey(String name, int pos) {
        this.name = name;
        this.pos = pos;
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
