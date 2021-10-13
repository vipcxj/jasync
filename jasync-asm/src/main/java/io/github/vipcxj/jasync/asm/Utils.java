package io.github.vipcxj.jasync.asm;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Utils {

    private static final ThreadLocal<Object> typesLocal = new ThreadLocal<>();
    private static final ThreadLocal<Object> elementsLocal = new ThreadLocal<>();

    public static void enterCompile(Object procEnv) {
        if (procEnv != null) {
            Class<?> procEnvClass = getProcEnvClass(procEnv.getClass());
            if (procEnvClass != null) {
                try {
                    Method getTypeUtils = procEnvClass.getMethod("getTypeUtils");
                    Method getElementUtils = procEnvClass.getMethod("getElementUtils");
                    Object types = getTypeUtils.invoke(procEnv);
                    typesLocal.set(types);
                    System.out.println("find types.");
                    Object elements = getElementUtils.invoke(procEnv);
                    elementsLocal.set(elements);
                    System.out.println("find elements.");
                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void exitCompile() {
        typesLocal.set(null);
        elementsLocal.set(null);
        System.out.println("clean types and elements");
    }

    public static Class<?> getTargetClass(Class<?> toTest, String name) {
        if (toTest == null) {
            return null;
        }
        if (name.equals(toTest.getName())) {
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

    public static Class<?> getProcEnvClass(Class<?> toTest) {
        return getTargetClass(toTest, "javax.annotation.processing.ProcessingEnvironment");
    }

    public static Class<?> getJavaFileObjectClass(Class<?> toTest) {
        return getTargetClass(toTest, "javax.tools.JavaFileObject");
    }

}
