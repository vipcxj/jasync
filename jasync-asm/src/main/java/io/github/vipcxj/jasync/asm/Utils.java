package io.github.vipcxj.jasync.asm;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Utils {

    private static final String PROMISE_TYPES_FILE_PROPERTY = "jasync_promise_types_file";
    private static Set<String> promiseTypes;

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

    public static Class<?> getJavaFileObjectClass(Class<?> toTest) {
        return getTargetClass(toTest, "javax.tools.JavaFileObject");
    }

    public static boolean isJPromise(String typeName) {
        if (promiseTypes == null) {
            String filePath = System.getProperty(PROMISE_TYPES_FILE_PROPERTY);
            if (filePath == null) {
                throw new NullPointerException("Unable to find the promise types file property.");
            }
            Path path = new File(filePath).toPath();
            if (!Files.exists(path)) {
                throw new NullPointerException("The promise types file not exist: " + path + ".");
            }
            try {
                List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                promiseTypes = new HashSet<>(lines);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read the promise type file: " + path + ".");
            }
        }
        return promiseTypes.contains(typeName);
    }

}
