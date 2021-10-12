package io.github.vipcxj.jasync.asm;

public class Utils {

    public static Class<?> getJavaFileObjectClass(Class<?> toTest) {
        if (toTest == null) {
            return null;
        }
        if ("javax.tools.JavaFileObject".equals(toTest.getName())) {
            return toTest;
        }
        for (Class<?> anInterface : toTest.getInterfaces()) {
            Class<?> found = getJavaFileObjectClass(anInterface);
            if (found != null) {
                return found;
            }
        }
        return getJavaFileObjectClass(toTest.getSuperclass());
    }
}
