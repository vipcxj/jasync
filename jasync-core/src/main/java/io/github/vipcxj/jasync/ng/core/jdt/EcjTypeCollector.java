package io.github.vipcxj.jasync.ng.core.jdt;

import io.github.vipcxj.jasync.ng.core.ReflectHelper;
import io.github.vipcxj.jasync.ng.utils.Logger;

import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.*;

public class EcjTypeCollector {

//    private static JavaFileManager FILE_MANAGER;

    private static Object getJavaProject(Object ecjObject) {
        try {
            Class<?> inputClass = ecjObject.getClass();
            Field envField = ReflectHelper.getField(inputClass, "_env");
            if (envField == null) {
                Logger.error("Unable to find the field _env on class " + inputClass);
                return null;
            }
            envField.setAccessible(true);
            Object envObject = envField.get(ecjObject);
            if (envObject == null) {
                Logger.error("The value of field of _env is null");
                return null;
            }
            Field javaProjectField = ReflectHelper.getField(envObject.getClass(), "_javaProject");
            if (javaProjectField == null) {
                Logger.error("Unable to find the field _javaProject on class " + envObject.getClass());
                return null;
            }
            javaProjectField.setAccessible(true);
            return javaProjectField.get(envObject);
        } catch (Throwable t) {
            Logger.error(t);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> getOptions(Object project) {
        try {
            Class<?> projectClass = project.getClass();
            ClassLoader classLoader = projectClass.getClassLoader();
            Class<?> javaProjectClass = ReflectHelper.loadClass(classLoader, "org.eclipse.jdt.core.IJavaProject");
            try {
                Class<?> aptConfigClass = ReflectHelper.loadClass(classLoader, "org.eclipse.jdt.apt.core.util.AptConfig");
                try {
                    Method getProcessorOptions = aptConfigClass.getMethod("getProcessorOptions", javaProjectClass, boolean.class);
                    return (Map<String, String>) getProcessorOptions.invoke(null, project, false);
                } catch (NoSuchMethodException e) {
                    Method getProcessorOptions = aptConfigClass.getMethod("getProcessorOptions", javaProjectClass);
                    return (Map<String, String>) getProcessorOptions.invoke(null, project);
                }
            } catch (ClassNotFoundException e) {
                Class<?> aptConfigClass = ClassLoaderHelper.loadClass("io.github.vipcxj.jasync.ng.core.jdt.AptConfig", projectClass);
                Logger.info("class loader of AptConfig: " + aptConfigClass.getClassLoader().getClass());
                Logger.info("current context class loader: " + (Thread.currentThread().getContextClassLoader() != null ? Thread.currentThread().getContextClassLoader().getClass() : null));
                Method getProcessorOptions = aptConfigClass.getMethod("getProcessorOptions", javaProjectClass);
                return (Map<String, String>) getProcessorOptions.invoke(null, project);
            }
        } catch (Throwable t) {
            Logger.error(t);
            return Collections.emptyMap();
        }
    }

    public static JavaFileManager getFileManager(Object ecjObject) {
//        if (FILE_MANAGER != null) {
//            return FILE_MANAGER;
//        }
        try {
            Object project = getJavaProject(ecjObject);
            if (project == null) {
                Logger.error("Unable to get IJavaProject from object of class " + ecjObject.getClass());
                return null;
            }
            Class<?> fileManagerClass = ClassLoaderHelper.loadClass("io.github.vipcxj.jasync.ng.core.jdt.EclipseFileManager", project.getClass());
            Constructor<?> constructor = fileManagerClass.getConstructor(Locale.class, Charset.class);
            JavaFileManager fileManager = (JavaFileManager) constructor.newInstance(null, null);
            Map<String, String> options = getOptions(project);
            List<String> optionList = new ArrayList<>();
            for (Map.Entry<String, String> entry : options.entrySet()) {
                optionList.add(entry.getKey());
                optionList.add(entry.getValue());
            }
            Logger.info(String.join(" ", optionList));
            Iterator<String> iterator = optionList.iterator();
            while (iterator.hasNext()) {
                String next = iterator.next();
                if (fileManager.handleOption(next, iterator)) {
                    Logger.info("file manager has handled " + next);
                } else {
                    Logger.info("file manager unable to handle " + next);
                }
            }
            Iterable<JavaFileObject> fileObjects = fileManager.list(StandardLocation.CLASS_PATH, "", new HashSet<>(Collections.singletonList(JavaFileObject.Kind.CLASS)), true);
            for (JavaFileObject fileObject : fileObjects) {
                Logger.info(fileObject.toUri().getPath());
            }
//            FILE_MANAGER = fileManager;
            return fileManager;
        } catch (Throwable t) {
            Logger.error(t);
            return null;
        }
    }
}
