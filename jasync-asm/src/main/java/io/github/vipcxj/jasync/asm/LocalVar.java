package io.github.vipcxj.jasync.asm;

public class LocalVar {

    /** The name of a local variable. */
    private final String name;

    /** The type descriptor of this local variable. */
    private final String desc;

    /** The signature of this local variable. May be {@literal null}. */
    private final String signature;

    public LocalVar(String name, String desc, String signature) {
        this.name = name;
        this.desc = desc;
        this.signature = signature;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public String getSignature() {
        return signature;
    }
}
