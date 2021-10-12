package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.ClassReader;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Transformer {

    private static InputStream openInputStream(Class<?> javaFileObjectClass, Object fileObject) {
        try {
            Method method = javaFileObjectClass.getMethod("openInputStream");
            return (InputStream) method.invoke(fileObject);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void transform(Class<?> javaFileObjectClass, Object fileObject) {
        try (InputStream is = openInputStream(javaFileObjectClass, fileObject)) {
            ClassReader classReader = new ClassReader(is);
            classReader.accept(new ClassChecker(null), 0);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
