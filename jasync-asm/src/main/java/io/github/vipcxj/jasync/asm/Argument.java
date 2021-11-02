package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.tree.analysis.Value;

public class Argument {
    private final Value value;
    private final String name;

    public Argument(Value value, String name) {
        this.value = value;
        this.name = name;
    }

    public Value getValue() {
        return value;
    }

    public String getName() {
        return name;
    }
}
