package io.github.vipcxj.jasync.ng.utils.hack;

import io.github.vipcxj.jasync.ng.utils.hack.dummy.Parent;
import sun.misc.Unsafe;

import java.lang.reflect.*;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/*
 * Copyright (C) 2009-2020 The Project Lombok Authors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
@SuppressWarnings("unchecked")
public class Utils {

    public static Object getOwnModule(Class<?> selfType) {
        try {
            Method m = Permit.getMethod(Class.class, "getModule");
            return m.invoke(selfType);
        } catch (Exception e) {
            return null;
        }
    }

    public static Object getModule(String module) {
		/* call public api: ModuleLayer.boot().findModule("jdk.compiler").get();
		   but use reflection because we don't want this code to crash on jdk1.7 and below.
		   In that case, none of this stuff was needed in the first place, so we just exit via
		   the catch block and do nothing.
		 */

        try {
            Class<?> cModuleLayer = Class.forName("java.lang.ModuleLayer");
            Method mBoot = cModuleLayer.getDeclaredMethod("boot");
            Object bootLayer = mBoot.invoke(null);
            Class<?> cOptional = Class.forName("java.util.Optional");
            Method mFindModule = cModuleLayer.getDeclaredMethod("findModule", String.class);
            Object oCompilerO = mFindModule.invoke(bootLayer, module);
            return cOptional.getDeclaredMethod("get").invoke(oCompilerO);
        } catch (Exception e) {
            return null;
        }
    }

    public static void addOpensFromJdkCompilerModule(Class<?> type, String[] packages) {
        addOpens(getOwnModule(type), getModule("jdk.compiler"), packages);
    }

    public static void addOpens(Class<?> ownClass, String moduleName, String[] packages) {
        addOpens(getOwnModule(ownClass), getModule(moduleName), packages);
    }

    /** Useful from jdk9 and up; required from jdk16 and up. This code is supposed to gracefully do nothing on jdk8 and below, as this operation isn't needed there. */
    public static void addOpens(Object ownModule, Object targetModule, String[] packages) {
        Class<?> cModule;
        try {
            cModule = Class.forName("java.lang.Module");
        } catch (ClassNotFoundException e) {
            return; //jdk8-; this is not needed.
        }
        Unsafe unsafe = getUnsafe();
        if (unsafe == null) {
            throw new IllegalStateException("Unable to get the unsafe object.");
        }
        if (targetModule == null) {
            throw new IllegalStateException("Target module should not be null.");
        }
        if (ownModule == null) {
            throw new IllegalStateException("Own module should not be null.");
        }
        try {
            Method m = cModule.getDeclaredMethod("implAddOpens", String.class, cModule);
            long firstFieldOffset = getFirstFieldOffset(unsafe);
            unsafe.putBooleanVolatile(m, firstFieldOffset, true);
            for (String p : packages) //noinspection JavaReflectionInvocation
                m.invoke(targetModule, p, ownModule);
        } catch (Exception ignore) {}
    }

    private static long getFirstFieldOffset(Unsafe unsafe) {
        try {
            return unsafe.objectFieldOffset(Parent.class.getDeclaredField("first"));
        } catch (NoSuchFieldException | SecurityException e) {
            // can't happen.
            throw new RuntimeException(e);
        } // can't happen

    }

    private static Unsafe getUnsafe() {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            return (Unsafe) theUnsafe.get(null);
        } catch (Exception e) {
            return null;
        }
    }

/*    public static JavacProcessingEnvironment getJavacProcessingEnvironment(Object procEnv, ProcessingEnvironment originalProcEnv) {
        if (procEnv instanceof JavacProcessingEnvironment) return (JavacProcessingEnvironment) procEnv;

        // try to find a "delegate" field in the object, and use this to try to obtain a JavacProcessingEnvironment
        for (Class<?> procEnvClass = procEnv.getClass(); procEnvClass != null; procEnvClass = procEnvClass.getSuperclass()) {
            Object delegate = tryGetDelegateField(procEnvClass, procEnv);
            if (delegate == null) delegate = tryGetProxyDelegateToField(procEnvClass, procEnv);
            if (delegate == null) delegate = tryGetProcessingEnvField(procEnvClass, procEnv);

            if (delegate != null) return getJavacProcessingEnvironment(delegate, originalProcEnv);
            // delegate field was not found, try on superclass
        }
        String errMessage = "Can't get the delegate of the gradle IncrementalProcessingEnvironment. JAsync won't work.";
        originalProcEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                "Can't get the delegate of the gradle IncrementalProcessingEnvironment. JAsync won't work.");
        throw new IllegalArgumentException(errMessage);
    }*/

/*    *//**
     * Gradle incremental processing
     *//*
    private static Object tryGetDelegateField(Class<?> delegateClass, Object instance) {
        try {
            return Permit.getField(delegateClass, "delegate").get(instance);
        } catch (Exception e) {
            return null;
        }
    }

    *//**
     * Kotlin incremental processing
     *//*
    private static Object tryGetProcessingEnvField(Class<?> delegateClass, Object instance) {
        try {
            return Permit.getField(delegateClass, "processingEnv").get(instance);
        } catch (Exception e) {
            return null;
        }
    }

    *//**
     * IntelliJ IDEA >= 2020.3
     */
    public static Object tryGetProxyDelegateToField(Object instance) {
        try {
            InvocationHandler handler = Proxy.getInvocationHandler(instance);
            return Permit.getField(handler.getClass(), "val$delegateTo").get(handler);
        } catch (Exception e) {
            return null;
        }
    }

    public static Field unsafeGetField(Class<?> targetClass, String name) {
        try {
            Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
            getDeclaredFields0.setAccessible(true);
            Field[] unfilteredFields = (Field[]) getDeclaredFields0.invoke(targetClass, false);
            return Arrays.stream(unfilteredFields).filter(f -> Objects.equals(f.getName(), name)).findAny().orElse(null);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Map<String, Certificate[]> getCertsMap(ClassLoader classLoader) throws NoSuchFieldException, IllegalAccessException {
        Field package2certs = unsafeGetField(ClassLoader.class, "package2certs");
        if (package2certs == null) {
            throw new NoSuchFieldException("No such field named package2certs in class ClassLoader.");
        }
        package2certs.setAccessible(true);
        return  (Map<String, Certificate[]>) package2certs.get(classLoader);
    }

    public static Certificate[] unsecureClassloader(ClassLoader classLoader, String className) throws NoSuchFieldException, IllegalAccessException {
        Map<String, Certificate[]> certsMap = getCertsMap(classLoader);
        if (certsMap != null) {
            int i = className.lastIndexOf('.');
            String packageName = (i == -1) ? "" : className.substring(0, i);
            return certsMap.put(packageName, new Certificate[0]);
        }
        return null;
    }

    public static void secureClassloader(ClassLoader classLoader, String className, Certificate[] certs) throws NoSuchFieldException, IllegalAccessException {
        Map<String, Certificate[]> certsMap = getCertsMap(classLoader);
        if (certsMap != null) {
            int i = className.lastIndexOf('.');
            String packageName = (i == -1) ? "" : className.substring(0, i);
            certsMap.put(packageName, certs);
        }
    }

}
