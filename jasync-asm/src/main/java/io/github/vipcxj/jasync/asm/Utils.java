package io.github.vipcxj.jasync.asm;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentSkipListSet;

public class Utils {

    private static final ThreadLocal<Stack<Object>> typesLocal = new ThreadLocal<>();
    private static final ThreadLocal<Stack<Object>> elementsLocal = new ThreadLocal<>();
    private static final Set<String> promiseTypes = new ConcurrentSkipListSet<>();

    public static void enterCompile(Object procEnv) {
        if (procEnv != null) {
            Class<?> procEnvClass = getProcEnvClass(procEnv.getClass());
            if (procEnvClass != null) {
                try {
                    Method getTypeUtils = procEnvClass.getMethod("getTypeUtils");
                    Method getElementUtils = procEnvClass.getMethod("getElementUtils");
                    Object types = getTypeUtils.invoke(procEnv);
                    Stack<Object> typesStack = typesLocal.get();
                    if (typesStack == null) {
                        typesStack = new Stack<>();
                        typesLocal.set(typesStack);
                    }
                    typesStack.push(types);
                    Stack<Object> elementsStack = elementsLocal.get();
                    if (elementsStack == null) {
                        elementsStack = new Stack<>();
                        elementsLocal.set(elementsStack);
                    }
                    Object elements = getElementUtils.invoke(procEnv);
                    elementsStack.push(elements);
                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void exitCompile() {
        Stack<Object> typesStack = typesLocal.get();
        if (typesStack != null) {
            typesStack.pop();
        }
        Stack<Object> elementsStack = elementsLocal.get();
        if (elementsStack != null) {
            elementsStack.pop();
        }
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

    public static Class<?> getProcEnvClass(Class<?> toTest) {
        return getTargetClass(toTest, "javax.annotation.processing.ProcessingEnvironment");
    }

    public static Class<?> getJavaFileObjectClass(Class<?> toTest) {
        return getTargetClass(toTest, "javax.tools.JavaFileObject");
    }

    private static Class<?> getElementsClass(Class<?> implClass) {
        return getTargetClass(implClass, "javax.lang.model.util.Elements");
    }

    private static Class<?> getTypesClass(Class<?> implClass) {
        return getTargetClass(implClass, "javax.lang.model.util.Types");
    }

    private static Class<?> getTypeElementClass(Class<?> implClass) {
        return getTargetClass(implClass, "javax.lang.model.element.TypeElement");
    }

    private static Object getElements() {
        Stack<Object> stack = elementsLocal.get();
        if (stack != null && !stack.isEmpty()) {
            return stack.peek();
        } else {
            return null;
        }
    }

    private static Object getTypes() {
        Stack<Object> stack = typesLocal.get();
        if (stack != null && !stack.isEmpty()) {
            return stack.peek();
        } else {
            return null;
        }
    }

    private static Object getType(String typeName) {
        Object elements = getElements();
        if (elements == null) {
            throw new IllegalStateException("No elements found. So unable to get the type of " + typeName + ".");
        }
        Class<?> elementsClass = getElementsClass(elements.getClass());
        try {
            Method  getTypeElement = elementsClass.getMethod("getTypeElement", CharSequence.class);
            Object element = getTypeElement.invoke(elements, typeName);
            if (element == null) {
                throw new IllegalArgumentException("Unable to find a element named of " + typeName + ".");
            }
            Class<?> typeElementClass = getTypeElementClass(element.getClass());
            Method asType = typeElementClass.getMethod("asType");
            return asType.invoke(element);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Unable to get the type of " + typeName + ".", e);
        }
    }

    public static boolean isJPromise(String typeName) {
        if (promiseTypes.contains(typeName)) {
            return true;
        }
        if ("void".equals(typeName)
                || "int".equals(typeName)
                || "long".equals(typeName)
                || "short".equals(typeName)
                || "float".equals(typeName)
                || "double".equals(typeName)
                || "char".equals(typeName)
                || "boolean".equals(typeName)
                || "byte".equals(typeName)
        ) {
            return false;
        }
        Object promiseType = getType(Constants.JPROMISE_DESC.getClassName());
        Object testType = getType(typeName);
        Class<?> typeMirrorClass = getTargetClass(promiseType.getClass(), "javax.lang.model.type.TypeMirror");
        Object types = getTypes();
        if (types == null) {
            throw new IllegalStateException("No types found. So unable to check whether type " + typeName + " is a promise type.");
        }
        Class<?> typesClass = getTypesClass(types.getClass());
        try {
            Method isSubtype = typesClass.getMethod("isSubtype", typeMirrorClass, typeMirrorClass);
            boolean result = (Boolean) isSubtype.invoke(types, testType, promiseType);
            if (result) {
                promiseTypes.add(typeName);
            }
            return result;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Unable to check whether type " + typeName + " is a promise type.", e);
        }
    }

}
