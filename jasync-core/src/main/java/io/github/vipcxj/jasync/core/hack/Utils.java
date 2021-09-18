package io.github.vipcxj.jasync.core.hack;

import io.github.vipcxj.jasync.core.hack.dummy.Parent;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Utils {

    private static Object getOwnModule(Class<?> selfType) {
        try {
            Method m = Permit.getMethod(Class.class, "getModule");
            return m.invoke(selfType);
        } catch (Exception e) {
            return null;
        }
    }

    private static Object getJdkCompilerModule() {
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
            Object oCompilerO = mFindModule.invoke(bootLayer, "jdk.compiler");
            return cOptional.getDeclaredMethod("get").invoke(oCompilerO);
        } catch (Exception e) {
            return null;
        }
    }

    /** Useful from jdk9 and up; required from jdk16 and up. This code is supposed to gracefully do nothing on jdk8 and below, as this operation isn't needed there. */
    public static void addOpensForLombok(Class<?> type, String[] packages) {
        Class<?> cModule;
        try {
            cModule = Class.forName("java.lang.Module");
        } catch (ClassNotFoundException e) {
            return; //jdk8-; this is not needed.
        }
        Unsafe unsafe = getUnsafe();
        Object jdkCompilerModule = getJdkCompilerModule();
        Object ownModule = getOwnModule(type);
        if (unsafe == null) {
            throw new IllegalStateException("Unable to get the unsafe object.");
        }
        if (jdkCompilerModule == null) {
            throw new IllegalStateException("Unable to get the jdk compiler module object.");
        }
        if (ownModule == null) {
            throw new IllegalStateException("Unable to get the own module object.");
        }
        try {
            Method m = cModule.getDeclaredMethod("implAddOpens", String.class, cModule);
            long firstFieldOffset = getFirstFieldOffset(unsafe);
            unsafe.putBooleanVolatile(m, firstFieldOffset, true);
            for (String p : packages) m.invoke(jdkCompilerModule, p, ownModule);
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
}
