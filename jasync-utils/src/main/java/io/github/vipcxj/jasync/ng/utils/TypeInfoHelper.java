package io.github.vipcxj.jasync.ng.utils;

public class TypeInfoHelper {

    private static final String[] EMPTY_ARRAY = new String[0];

    public static String[] typeInfo(String superName) {
        return typeInfo(superName, EMPTY_ARRAY);
    }

    public static String[] typeInfo(String superName, String[] interfaces) {
        String[] typeInfo = new String[interfaces.length + 1];
        typeInfo[0] = superName;
        System.arraycopy(interfaces, 0, typeInfo, 1, interfaces.length);
        return typeInfo;
    }

    public static String getSuperName(String[] typeInfo) {
        assert typeInfo.length >= 1;
        return typeInfo[0];
    }

    public static String[] getInterfaces(String[] typeInfo) {
        assert typeInfo.length >= 1;
        if (typeInfo.length == 1) {
            return EMPTY_ARRAY;
        }
        String[] interfaces = new String[typeInfo.length - 1];
        System.arraycopy(typeInfo, 1, interfaces, 0, interfaces.length);
        return interfaces;
    }

    public static String print(String className, String[] typeInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append(className).append(",");
        String superName = getSuperName(typeInfo);
        sb.append((superName == null || "java.lang.Object".equals(superName)) ? "" : superName).append(",");
        for (int i = 1; i < typeInfo.length; ++i) {
            String itf = typeInfo[i];
            sb.append(itf != null ? itf : "").append(",");
        }
        return sb.toString();
    }
}
