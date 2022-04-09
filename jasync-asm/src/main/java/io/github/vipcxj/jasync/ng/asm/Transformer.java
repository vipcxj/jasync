package io.github.vipcxj.jasync.ng.asm;

import io.github.vipcxj.jasync.ng.utils.Logger;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class Transformer {

    public static void transform(Object fileObject) {
        Logger.info("Transforming " + ReflectObjectHelper.toUri(fileObject));
        try (InputStream is = ReflectObjectHelper.openInputStream(fileObject)) {
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
                    try (OutputStream fos = ReflectObjectHelper.openOutputStream(fileObject)) {
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
