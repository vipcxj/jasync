package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class Utils {

    private static final String FULL_TYPES_FILE_PROPERTY = "jasync_full_types_file";
    private static final String USE_CLASS_LOADER_PROPERTY = "jasync_use_class_loader";
    private static final Set<String> promiseTypes = new ConcurrentSkipListSet<>();
    private static Map<String, TypeInfo> typeInfoMap;
    private static final boolean USE_CLASS_LOADER = isUseClassLoader();

    private static Object getEnum(Class<?> enumClass, String name) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method valueOf = Enum.class.getMethod("valueOf", Class.class, String.class);
        return valueOf.invoke(null, enumClass, name);
    }

    private static Iterable<Object> listFiles(Object javaFileManager) {
        try {
            Class<?> javaFileManagerClass = getJavaFileManagerClass(javaFileManager.getClass());
            Class<?> standardLocationClass = javaFileManagerClass.getClassLoader().loadClass("javax.tools.StandardLocation");
            Class<?> locationClass = javaFileManagerClass.getClassLoader().loadClass("javax.tools.JavaFileManager$Location");
            Class<?> kindClass = javaFileManagerClass.getClassLoader().loadClass("javax.tools.JavaFileObject$Kind");
            Object classPath = getEnum(standardLocationClass, "CLASS_PATH");
            Object classKind = getEnum(kindClass, "CLASS");
            Method list = javaFileManagerClass.getMethod("list", locationClass, String.class, Set.class, boolean.class);
            //noinspection unchecked
            return (Iterable<Object>) list.invoke(javaFileManager, classPath, "", Collections.singleton(classKind), true);
        } catch (Throwable t) {
            Logger.error("Unable to list the files from class path.");
            Logger.error(t);
            return Collections.emptyList();
        }
    }

    private static Method toUri;

    static URI toUri(Class<?> javaFileObjectClass, Object javaFileObject) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        if (toUri == null) {
            toUri = javaFileObjectClass.getMethod("toUri");
        }
        return (URI) toUri.invoke(javaFileObject);
    }

    private static Method openInputStream;

    static InputStream openInputStream(Class<?> javaFileObjectClass, Object fileObject) {
        try {
            if (openInputStream == null) {
                openInputStream = javaFileObjectClass.getMethod("openInputStream");
            }
            return (InputStream) openInputStream.invoke(fileObject);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Method openOutputStream;

    static OutputStream openOutputStream(Class<?> javaFileObjectClass, Object fileObject) {
        try {
            if (openOutputStream == null) {
                openOutputStream = javaFileObjectClass.getMethod("openOutputStream");
            }
            return (OutputStream) openOutputStream.invoke(fileObject);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static final String OBJECT_PROMISE_NAME = Constants.JPROMISE_DESC.getClassName();
    private static final String OBJECT_BINARY_NAME = "java.lang.Object";
    private static final String NULL_BINARY_NAME = "null";

    private static Map<String, TypeInfo> getTypeInfoMapOutside() {
        String filePath = System.getProperty(FULL_TYPES_FILE_PROPERTY);
        if (filePath == null) {
            throw new NullPointerException("Unable to find the full types file property.");
        }
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new NullPointerException("The full types file not exist: " + path + ".");
        }
        try {
            Map<String, TypeInfo> map = new HashMap<>();
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line != null && !line.isEmpty()) {
                    String[] parts = line.split(",");
                    if (parts.length > 0) {
                        String name = parts[0];
                        if (!name.isEmpty()) {
                            String superName = parts.length > 1 ? parts[1] : OBJECT_BINARY_NAME;
                            if (superName.isEmpty()) {
                                superName = name.equals(OBJECT_BINARY_NAME) ? null : OBJECT_BINARY_NAME;
                            }
                            String[] interfaces;
                            if (parts.length > 2) {
                                List<String> names = new ArrayList<>(parts.length - 2);
                                for (int i = 2; i < parts.length; ++i) {
                                    String part = parts[i];
                                    if (!part.isEmpty()) {
                                        names.add(part);
                                    }
                                }
                                interfaces = names.toArray(new String[0]);
                            } else {
                                interfaces = new String[0];
                            }
                            map.put(name, new TypeInfo(superName, interfaces));
                        }
                    }
                }
            }
            return map;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read the promise type file: " + path + ".");
        }
    }

    static void collectTypes(Object javaFileManager) {
        if (typeInfoMap == null) {
            typeInfoMap = new ConcurrentHashMap<>();
            try {
                Class<?> javaFileObjectClass = javaFileManager.getClass().getClassLoader()
                        .loadClass("javax.tools.JavaFileObject");
                for (Object fileObject : listFiles(javaFileManager)) {
                    try (InputStream is = openInputStream(javaFileObjectClass, fileObject)) {
                        ClassReader classReader = new ClassReader(is);
                        TypeScanner typeScanner = new TypeScanner(null);
                        classReader.accept(typeScanner, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
                        TypeInfo typeInfo = new TypeInfo(typeScanner.getSuperName(), typeScanner.getInterfaces());
                        typeInfoMap.put(typeScanner.getName(), typeInfo);
                    }
                }
                Map<String, TypeInfo> mapOutside = getTypeInfoMapOutside();
                typeInfoMap.putAll(mapOutside);
            } catch (Throwable t) {
                Logger.error("Fail to collect types.");
                Logger.error(t);
            }
        }
    }

    public static Map<String, TypeInfo> getTypeInfoMap() {
        if (typeInfoMap == null) {
            throw new IllegalStateException("The type info map is not initialized.");
        }
        return typeInfoMap;
    }

    private static boolean isUseClassLoader() {
        try {
            String property = System.getProperty(USE_CLASS_LOADER_PROPERTY);
            if (property == null) {
                return true;
            }
            property = property.trim();
            if ("off".equalsIgnoreCase(property) || "no".equalsIgnoreCase(property) || "0".equalsIgnoreCase(property) || "false".equalsIgnoreCase(property)) {
                return false;
            }
        } catch (Throwable t) {
            Logger.error("Unable to get the env \"jasync_use_class_loader\".");
            Logger.error(t);
        }
        return true;
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

    public static Class<?> getJavaFileManagerClass(Class<?> toTest) {
        return getTargetClass(toTest, "javax.tools.JavaFileManager");
    }

    public static Class<?> getJavaFileObjectClass(Class<?> toTest) {
        return getTargetClass(toTest, "javax.tools.JavaFileObject");
    }

    /**
     * check if the type is a promise type.
     * @param typeName the binary name
     * @return whether it is a promise type.
     */
    public static boolean isJPromise(String typeName) {
        if (promiseTypes.contains(typeName)) {
            return true;
        } else {
            int cmp = isSubTypeOf(typeName, OBJECT_PROMISE_NAME);
            if (cmp == 0) {
                throw new RuntimeException("Unable to decide whether " + typeName + " is a promise type.");
            } else if (cmp == 1) {
                Logger.info("Find promise type: " + typeName + ".");
                promiseTypes.add(typeName);
                return true;
            } else {
                return false;
            }
        }
    }

    private static Class<?> getClass(String type) {
        try {
            return Class.forName(type);
        } catch (ClassNotFoundException e) {
            Logger.error("Unable to find " + type + " in class loader " + Utils.class.getClassLoader());
            Logger.error(e);
            return null;
        }
    }

    private static String getSuperTypeFromClassLoader(String type) {
        if (!USE_CLASS_LOADER) {
            return null;
        }
        Class<?> aClass = getClass(type);
        if (aClass != null) {
            Class<?> superClass = aClass.getSuperclass();
            return superClass != null ? superClass.getName() : OBJECT_BINARY_NAME;
        } else {
            return null;
        }
    }

    private static String getSuperType(String type) {
        Map<String, TypeInfo> typeInfoMap = getTypeInfoMap();
        if (typeInfoMap == null) {
            return getSuperTypeFromClassLoader(type);
        }
        TypeInfo typeInfo = typeInfoMap.get(type);
        if (typeInfo == null) {
            return getSuperTypeFromClassLoader(type);
        }
        return typeInfo.getSuperName();
    }

    private static String[] getInterfacesFromClassLoader(String type) {
        if (!USE_CLASS_LOADER) {
            return null;
        }
        Class<?> aClass = getClass(type);
        if (aClass != null) {
            Class<?>[] interfaceClasses = aClass.getInterfaces();
            String[] interfaces = new String[interfaceClasses.length];
            for (int i = 0; i < interfaces.length; ++i) {
                interfaces[i] = interfaceClasses[i].getName();
            }
            return interfaces;
        } else {
            return null;
        }
    }

    private static String[] getInterfaces(String type) {
        Map<String, TypeInfo> typeInfoMap = getTypeInfoMap();
        if (typeInfoMap == null) {
            return getInterfacesFromClassLoader(type);
        }
        TypeInfo typeInfo = typeInfoMap.get(type);
        if (typeInfo == null) {
            return getInterfacesFromClassLoader(type);
        }
        return typeInfo.getInterfaces();
    }

    /**
     * Check whether type1 is the sub type of type2.
     * Only reference type is accept. The primitive type and array type are not accepted.
     * @param type1 the binary name of type1
     * @param type2 the binary name of type2
     * @return 1 if type1 is the sub type of type2, -1 if type1 is not the sub type of type2, 0 if unknown.
     */
    public static int isSubTypeOf(String type1, String type2) {
        if (OBJECT_BINARY_NAME.equals(type2)) {
            return 1;
        }
        if (OBJECT_BINARY_NAME.equals(type1)) {
            return -1;
        }
        if (NULL_BINARY_NAME.equals(type1)) {
            return 1;
        }
        if (NULL_BINARY_NAME.equals(type2)) {
            return -1;
        }
        if (Objects.equals(type1, type2)) {
            return 1;
        }
        boolean unknown = false;
        int test;
        // return null only if unknown or type1 is Object. But type1 is not Object here.
        String superType1 = getSuperType(type1);
        if (superType1 == null) {
            unknown = true;
        } else {
            test = isSubTypeOf(superType1, type2);
            if (test > 0) {
                return 1;
            } else if (test == 0) {
                unknown = true;
            }
        }
        String[] interfaces1 = getInterfaces(type1);
        if (interfaces1 == null) {
            unknown = true;
        } else {
            for (String anInterface : interfaces1) {
                test = isSubTypeOf(anInterface, type2);
                if (test > 0) {
                    return 1;
                } else if (test == 0) {
                    unknown = true;
                }
            }
        }
        return unknown ? 0 : -1;
    }

    private static String stepSuperName(Set<String> ancestors, String superName) {
        while (superName != null && !OBJECT_BINARY_NAME.equals(superName)) {
            if (ancestors.contains(superName)) {
                return superName;
            } else {
                ancestors.add(superName);
            }
            superName = getSuperType(superName);
        }
        return superName;
    }

    /**
     * Get the nearest common ancestors of the type1 and type2.
     * Only reference type is accept. The primitive type and array type are not accepted.
     * @param type1 the binary name of type1
     * @param type2 the binary name of type2
     * @return the nearest common ancestors of the type1 and type2 or null if undecided.
     */
    public static String getNearestCommonAncestor(String type1, String type2) {
        if (Objects.equals(type1, type2)) {
            return type1;
        }
        if (OBJECT_BINARY_NAME.equals(type1) || OBJECT_BINARY_NAME.equals(type2)) {
            return OBJECT_BINARY_NAME;
        }
        if (NULL_BINARY_NAME.equals(type1)) {
            return type2;
        }
        if (NULL_BINARY_NAME.equals(type2)) {
            return type1;
        }
        if (isSubTypeOf(type1, type2) == 1) {
            return type2;
        }
        if (isSubTypeOf(type2, type1) == 1) {
            return type1;
        }
        Set<String> ancestors = new HashSet<>();
        stepSuperName(ancestors, type1);
        return stepSuperName(ancestors, type2);
    }
}
