package io.github.vipcxj.jasync.ng.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectHelper {

    private static boolean isValidSuperClass(Class<?> type) {
        return type != null && type != Object.class;
    }

    public static Field getField(Class<?> type, String name) {
        try {
            return type.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            Class<?> superclass = type.getSuperclass();
            if (isValidSuperClass(superclass)) {
                return getField(superclass, name);
            } else {
                return null;
            }
        }
    }

    public static Method getMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            return type.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            Class<?> superclass = type.getSuperclass();
            if (isValidSuperClass(superclass)) {
                return getMethod(superclass, name, parameterTypes);
            } else {
                return null;
            }
        }
    }

    public static Class<?> loadClass(ClassLoader classLoader, String name) throws ClassNotFoundException {
        try {
            return getClassLoader(classLoader).loadClass(name);
        } catch (ClassNotFoundException e) {
            return getClassLoader(Thread.currentThread().getContextClassLoader()).loadClass(name);
        }
    }

    public static ClassLoader getClassLoader(ClassLoader classLoader) {
        return getClassLoader(classLoader, null);
    }

    public static ClassLoader getClassLoader(ClassLoader classLoader, ClassLoader defaultClassLoader) {
        return classLoader != null ? classLoader : (defaultClassLoader != null ? defaultClassLoader : ClassLoader.getSystemClassLoader());
    }
}
