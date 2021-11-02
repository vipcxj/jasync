package io.github.vipcxj.jasync.asm;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
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

    private static OutputStream openOutputStream(Class<?> javaFileObjectClass, Object fileObject) {
        try {
            Method method = javaFileObjectClass.getMethod("openOutputStream");
            return (OutputStream) method.invoke(fileObject);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void transform(Object fileObject) {
        Class<?> javaFileObjectClass = Utils.getJavaFileObjectClass(fileObject.getClass());
        try (InputStream is = openInputStream(javaFileObjectClass, fileObject)) {
            try (ByteArrayOutputStream os = new ByteArrayOutputStream()){
                byte[] buffer = new byte[128 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
                byte[] bytes = os.toByteArray();
                byte[] newBytes = JAsyncTransformer.transform(bytes);
                if (newBytes != bytes) {
                    try (OutputStream fos = openOutputStream(javaFileObjectClass, fileObject)) {
                        fos.write(newBytes);
                        fos.flush();
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
