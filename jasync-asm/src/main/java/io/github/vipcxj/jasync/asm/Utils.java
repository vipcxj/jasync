package io.github.vipcxj.jasync.asm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Utils {

    private static final String PROMISE_TYPES_FILE_PROPERTY = "jasync_promise_types_file";
    private static final String FULL_TYPES_FILE_PROPERTY = "jasync_full_types_file";
    private static Set<String> promiseTypes;
    private static Map<String, TypeInfo> typeInfoMap;

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

    private static Set<String> getPromiseTypes() {
        if (promiseTypes == null) {
            String filePath = System.getProperty(PROMISE_TYPES_FILE_PROPERTY);
            if (filePath == null) {
                throw new NullPointerException("Unable to find the promise types file property.");
            }
            Path path = Paths.get(filePath);
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
        return promiseTypes;
    }

    /**
     * check if the type is a promise type.
     * @param typeName the binary name
     * @return whether it is a promise type.
     */
    public static boolean isJPromise(String typeName) {
        return getPromiseTypes().contains(typeName);
    }

    private static final String OBJECT_BINARY_NAME = "java.lang.Object";

    private static Map<String, TypeInfo> getTypeInfoMap() {
        if (typeInfoMap == null) {
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
                typeInfoMap = map;
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read the promise type file: " + path + ".");
            }
        }
        return typeInfoMap;
    }

    private static Class<?> getClass(String type) {
        try {
            return Class.forName(type);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static String getSuperTypeFromClassLoader(String type) {
        Class<?> aClass = getClass(type);
        return aClass != null ? aClass.getSuperclass().getName() : null;
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

    private static String stepInterfaces(Set<String> ancestors, String typeName) {
        if (OBJECT_BINARY_NAME.equals(typeName)) {
            return OBJECT_BINARY_NAME;
        }
        String[] interfaces = getInterfaces(typeName);
        if (interfaces == null) {
            return null;
        }
        boolean unknown = false;
        List<String> nextInterfaces = new ArrayList<>(Arrays.asList(interfaces));
        while (!nextInterfaces.isEmpty()) {
            List<String> newNextInterfaces = new ArrayList<>();
            for (String anInterface : nextInterfaces) {
                if (ancestors.contains(anInterface)) {
                    return anInterface;
                } else {
                    ancestors.add(anInterface);
                }
                String[] interfacesArray = getInterfaces(anInterface);
                if (interfacesArray == null) {
                    unknown = true;
                } else {
                    newNextInterfaces.addAll(Arrays.asList(interfacesArray));
                }
            }
            nextInterfaces = newNextInterfaces;
        }
        return unknown ? null : OBJECT_BINARY_NAME;
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
        Set<String> ancestors = new HashSet<>();
        stepSuperName(ancestors, type1);
        String root = stepSuperName(ancestors, type2);
        boolean unknown = false;
        if (root == null) {
            unknown = true;
        } else if (!OBJECT_BINARY_NAME.equals(root)) {
            return root;
        }
        root = stepInterfaces(ancestors, type1);
        if (root == null) {
            unknown = true;
        } else if (!OBJECT_BINARY_NAME.equals(root)) {
            return root;
        }
        root = stepInterfaces(ancestors, type2);
        if (root == null) {
            unknown = true;
        } else if (!OBJECT_BINARY_NAME.equals(root)) {
            return root;
        }
        return unknown ? null : OBJECT_BINARY_NAME;
    }
}
