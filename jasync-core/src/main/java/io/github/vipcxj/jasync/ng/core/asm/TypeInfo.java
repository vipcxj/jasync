package io.github.vipcxj.jasync.ng.core.asm;

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

    public String print(String className) {
        StringBuilder sb = new StringBuilder();
        sb.append(className).append(",");
        sb.append((superName == null || "java.lang.Object".equals(superName)) ? "" : superName).append(",");
        for (String itf : interfaces) {
            sb.append(itf != null ? itf : "").append(",");
        }
        return sb.toString();
    }
}
