package io.github.vipcxj.jasync.core.jdt;

import io.github.vipcxj.jasync.core.ReflectHelper;

import javax.tools.JavaFileManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class DebugTool {

    private static Iterable<Path> extractPathIterable(Object obj) {
        try {
            Method getPaths = ReflectHelper.getMethod(obj.getClass(), "getPaths");
            assert getPaths != null;
            getPaths.setAccessible(true);
            //noinspection unchecked
            return (Iterable<Path>) getPaths.invoke(obj);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Iterable<Path>> getPathMap(Object fileManager) {
        Class<?> eclipseFileManagerClass;
        try {
            eclipseFileManagerClass = ReflectHelper.loadClass(fileManager.getClass().getClassLoader(),"org.eclipse.jdt.internal.compiler.tool.EclipseFileManager");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        Field field = ReflectHelper.getField(eclipseFileManagerClass, "locationHandler");
        if (field == null) {
            throw new NullPointerException("No such field: locationHandler in class EclipseFileManager.");
        }
        field.setAccessible(true);
        try {
            Object locationHandler = field.get(fileManager);
            Class<?> moduleLocationHandlerClass = ReflectHelper.loadClass(locationHandler.getClass().getClassLoader(), "org.eclipse.jdt.internal.compiler.tool.ModuleLocationHandler");
            Field field1 = ReflectHelper.getField(moduleLocationHandlerClass, "containers");
            if (field1 == null) {
                throw new NullPointerException("No such field: containers in class ModuleLocationHandler.");
            }
            field1.setAccessible(true);
            //noinspection unchecked
            Map<JavaFileManager.Location, Object> containers = (Map<JavaFileManager.Location, Object>) field1.get(locationHandler);
            Map<String, Iterable<Path>> out = new HashMap<>();
            if (containers != null) {
                for (Map.Entry<JavaFileManager.Location, Object> entry : containers.entrySet()) {
                    out.put(entry.getKey().getName(), extractPathIterable(entry.getValue()));
                }
            }
            return out;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
