package io.github.vipcxj.jasync.ng.asm;

import io.github.vipcxj.jasync.ng.utils.Logger;
import io.github.vipcxj.jasync.ng.utils.TypeInfoHelper;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class Utils {

    private static final String USE_CLASS_LOADER_PROPERTY = "jasync_use_class_loader";
    private static final Set<String> promiseTypes = new HashSet<>();
    private static final boolean USE_CLASS_LOADER = isUseClassLoader();

    private static final String OBJECT_PROMISE_NAME = Constants.JPROMISE_DESC.getClassName();
    private static final String OBJECT_BINARY_NAME = "java.lang.Object";
    private static final String NULL_BINARY_NAME = "null";

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

    private static String getPackageName(String type) {
        assert type != null;
        int index = type.lastIndexOf(".");
        return index != -1 ? type.substring(0, index) : "";
    }

    private static String[] getTypeInfo(String type) {
        Map<String, String[]> typeInfoMap = Globals.getTypeInfoMap();
        if (typeInfoMap == null) {
            return null;
        }
        String[] typeInfo = typeInfoMap.get(type);
        if (typeInfo == null) {
            typeInfoMap = collectTypes(getPackageName(type));
            typeInfo = typeInfoMap.get(type);
        }
        return typeInfo;
    }

    private static String getSuperType(String type) {
        String[] typeInfo = getTypeInfo(type);
        if (typeInfo == null) {
            return getSuperTypeFromClassLoader(type);
        }
        return TypeInfoHelper.getSuperName(typeInfo);
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
        String[] typeInfo = getTypeInfo(type);
        if (typeInfo == null) {
            return getInterfacesFromClassLoader(type);
        }
        return TypeInfoHelper.getInterfaces(typeInfo);
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

    private static Map<String, String[]> collectTypes(String packageName) {
        Map<String, String[]> typeInfoMap = Globals.getTypeInfoMap();
        if (packageName.startsWith("java.")) {
            return typeInfoMap;
        }
        Logger.info("Collect types from package: " + packageName);
        Object fileManager = Globals.getFileManager();
        try {
            Iterable<Object> fileObjects = ReflectObjectHelper.listFileObject(fileManager, "CLASS_PATH", packageName);
            for (Object fileObject : fileObjects) {
                // Logger.info("Class in class path: " + fileObject.toUri());
                try (InputStream is = ReflectObjectHelper.openInputStream(fileObject)) {
                    ClassReader classReader = new ClassReader(is);
                    TypeScanner typeScanner = new TypeScanner(null);
                    classReader.accept(typeScanner, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
                    typeInfoMap.put(typeScanner.getName(), TypeInfoHelper.typeInfo(typeScanner.getSuperName(), typeScanner.getInterfaces()));
                }
            }
            return typeInfoMap;
        } catch (IOException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to collect types.", e);
        }
    }

    public static void addManyMap(List<Integer> map, int value, int num) {
        for (int i = 0; i < num; ++i) {
            map.add(value);
        }
    }
}
