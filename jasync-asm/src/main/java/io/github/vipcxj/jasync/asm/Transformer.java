package io.github.vipcxj.jasync.asm;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class Transformer {

    private static Object unwrapProxy(Object maybeProxy) {
        Object delegate = io.github.vipcxj.jasync.utils.hack.Utils.tryGetProxyDelegateToField(maybeProxy);
        Object unwrapped = maybeProxy;
        while (delegate != null) {
            unwrapped = delegate;
            delegate = io.github.vipcxj.jasync.utils.hack.Utils.tryGetProxyDelegateToField(delegate);
        }
        return unwrapped;
    }

    public static void transform(Object fileManager, Object fileObject) {
        fileManager = unwrapProxy(fileManager);
        Utils.collectTypes(fileManager);
        Class<?> javaFileObjectClass = Utils.getJavaFileObjectClass(fileObject.getClass());
        try (InputStream is = Utils.openInputStream(javaFileObjectClass, fileObject)) {
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
                    try (OutputStream fos = Utils.openOutputStream(javaFileObjectClass, fileObject)) {
                        fos.write(newBytes);
                        fos.flush();
                    }
                }
            }
        } catch (Throwable t) {
            Logger.error(t);
        }
    }
}
