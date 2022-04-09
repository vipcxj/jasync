package io.github.vipcxj.jasync.ng.asm;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collections;
import java.util.Set;

public class ReflectObjectHelper {

    private static Method TO_URI;

    static URI toUri(Object javaFileObject) {
        try {
            if (TO_URI == null) {
                Class<?> javaFileObjectClass = getJavaFileObjectClass(javaFileObject.getClass());
                TO_URI = javaFileObjectClass.getMethod("toUri");
            }
            return (URI) TO_URI.invoke(javaFileObject);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Method OPEN_INPUT_STREAM;

    static InputStream openInputStream(Object fileObject) {
        try {
            if (OPEN_INPUT_STREAM == null) {
                Class<?> javaFileObjectClass = getJavaFileObjectClass(fileObject.getClass());
                OPEN_INPUT_STREAM = javaFileObjectClass.getMethod("openInputStream");
            }
            return (InputStream) OPEN_INPUT_STREAM.invoke(fileObject);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Method OPEN_OUTPUT_STREAM;

    static OutputStream openOutputStream(Object fileObject) {
        try {
            if (OPEN_OUTPUT_STREAM == null) {
                Class<?> javaFileObjectClass = getJavaFileObjectClass(fileObject.getClass());
                OPEN_OUTPUT_STREAM = javaFileObjectClass.getMethod("openOutputStream");
            }
            return (OutputStream) OPEN_OUTPUT_STREAM.invoke(fileObject);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static Class<?> getTargetClass(Class<?> toTest, String name) {
        if (toTest == null) {
            return null;
        }
        if (name.equals(toTest.getName())) {
            return toTest;
        }
        for (Class<?> anInterface : toTest.getInterfaces()) {
            Class<?> found = getTargetClass(anInterface, name);
            if (found != null) {
                return found;
            }
        }
        return getTargetClass(toTest.getSuperclass(), name);
    }

    private static Class<?> JAVA_FILE_OBJECT_CLASS = null;
    public static Class<?> getJavaFileObjectClass(Class<?> toTest) {
        if (JAVA_FILE_OBJECT_CLASS == null) {
            JAVA_FILE_OBJECT_CLASS = getTargetClass(toTest, "javax.tools.JavaFileObject");
        }
        return JAVA_FILE_OBJECT_CLASS;
    }

    private static Class<?> JAVA_FILE_MANAGER_CLASS = null;
    public static Class<?> getJavaFileManagerClass(Object instance) {
        if (JAVA_FILE_MANAGER_CLASS == null) {
            JAVA_FILE_MANAGER_CLASS = getTargetClass(instance.getClass(), "javax.tools.JavaFileManager");
        }
        return JAVA_FILE_MANAGER_CLASS;
    }

    private static Class<?> LOCATION_CLASS = null;
    private static Class<?> locationClass(ClassLoader classLoader) throws ClassNotFoundException {
        if (LOCATION_CLASS == null) {
            classLoader = classLoader != null ? classLoader : ClassLoader.getSystemClassLoader();
            LOCATION_CLASS = classLoader.loadClass("javax.tools.JavaFileManager$Location");
        }
        return LOCATION_CLASS;
    }

    private static Class<?> STANDARD_LOCATION_CLASS = null;
    private static Class<?> standardLocationClass(ClassLoader classLoader) throws ClassNotFoundException {
        if (STANDARD_LOCATION_CLASS == null) {
            classLoader = classLoader != null ? classLoader : ClassLoader.getSystemClassLoader();
            STANDARD_LOCATION_CLASS = classLoader.loadClass("javax.tools.StandardLocation");
        }
        return STANDARD_LOCATION_CLASS;
    }

    private static Method STANDARD_LOCATION_FOR_NAME = null;
    private static Method getStandardLocationForNameMethod(ClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException {
        if (STANDARD_LOCATION_FOR_NAME == null) {
            Class<?> standardLocationClass = standardLocationClass(classLoader);
            STANDARD_LOCATION_FOR_NAME = standardLocationClass.getMethod("locationFor", String.class);
        }
        return STANDARD_LOCATION_FOR_NAME;
    }

    private static Object standLocation(ClassLoader classLoader, String name) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method standardLocationForNameMethod = getStandardLocationForNameMethod(classLoader);
        return standardLocationForNameMethod.invoke(null, name);
    }

    private static Class<?> FILE_OBJECT_KIND_CLASS = null;
    private static Class<?> getFileObjectKindClass(ClassLoader classLoader) throws ClassNotFoundException {
        if (FILE_OBJECT_KIND_CLASS == null) {
            classLoader = classLoader != null ? classLoader : ClassLoader.getSystemClassLoader();
            FILE_OBJECT_KIND_CLASS = classLoader.loadClass("javax.tools.JavaFileObject$Kind");
        }
        return FILE_OBJECT_KIND_CLASS;
    }

    private static Set<Object> getClassKindSet(ClassLoader classLoader) throws ClassNotFoundException {
        //noinspection unchecked
        Object classKind = Enum.valueOf((Class<Enum>) getFileObjectKindClass(classLoader), "CLASS");
        return Collections.singleton(classKind);
    }

    private static Method JAVA_FILE_MANAGER_LIST = null;
    private static Method getJavaFileManagerListMethod(Class<?> javaFileManagerClass) throws ClassNotFoundException, NoSuchMethodException {
        if (JAVA_FILE_MANAGER_LIST == null) {
            Class<?> locationClass = locationClass(javaFileManagerClass.getClassLoader());
            JAVA_FILE_MANAGER_LIST = javaFileManagerClass.getMethod("list", locationClass, String.class, Set.class, boolean.class);
        }
        return JAVA_FILE_MANAGER_LIST;
    }

    public static Iterable<Object> listFileObject(Object fileManager, String location, String packageName) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> javaFileManagerClass = getJavaFileManagerClass(fileManager);
        Method javaFileManagerListMethod = getJavaFileManagerListMethod(javaFileManagerClass);
        Object standLocation = standLocation(javaFileManagerClass.getClassLoader(), location);
        //noinspection unchecked
        return (Iterable<Object>) javaFileManagerListMethod.invoke(fileManager, standLocation, packageName, getClassKindSet(javaFileManagerClass.getClassLoader()), false);
    }
}
