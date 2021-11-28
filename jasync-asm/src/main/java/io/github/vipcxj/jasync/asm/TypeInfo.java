package io.github.vipcxj.jasync.asm;

public class TypeInfo {
    private final String superName;
    private final String[] interfaces;

    public TypeInfo(String superName, String[] interfaces) {
        this.superName = superName;
        this.interfaces = interfaces;
    }

    public String getSuperName() {
        return superName;
    }

    public String[] getInterfaces() {
        return interfaces;
    }

}
