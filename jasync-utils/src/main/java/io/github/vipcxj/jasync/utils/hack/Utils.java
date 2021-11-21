package io.github.vipcxj.jasync.utils.hack;

import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import io.github.vipcxj.jasync.utils.hack.dummy.Parent;
import sun.misc.Unsafe;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

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

    public static JavacProcessingEnvironment getJavacProcessingEnvironment(Object procEnv, ProcessingEnvironment originalProcEnv) {
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
    }

    /**
     * Gradle incremental processing
     */
    private static Object tryGetDelegateField(Class<?> delegateClass, Object instance) {
        try {
            return Permit.getField(delegateClass, "delegate").get(instance);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Kotlin incremental processing
     */
    private static Object tryGetProcessingEnvField(Class<?> delegateClass, Object instance) {
        try {
            return Permit.getField(delegateClass, "processingEnv").get(instance);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * IntelliJ IDEA >= 2020.3
     */
    public static Object tryGetProxyDelegateToField(Class<?> delegateClass, Object instance) {
        try {
            InvocationHandler handler = Proxy.getInvocationHandler(instance);
            return Permit.getField(handler.getClass(), "val$delegateTo").get(handler);
        } catch (Exception e) {
            return null;
        }
    }

}
