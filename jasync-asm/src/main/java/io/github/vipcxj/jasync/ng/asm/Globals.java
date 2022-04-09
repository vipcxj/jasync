package io.github.vipcxj.jasync.ng.asm;

import io.github.vipcxj.jasync.ng.utils.Logger;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

import static io.github.vipcxj.jasync.ng.utils.hack.Permit.setAccessible;

public class Globals {

    private static final InheritableThreadLocal<Object> helperThreadLocal = new InheritableThreadLocal<>();
    private static final String GLOBAL_THREAD_LOCAL_CLASS_NAME = "io.github.vipcxj.jasync.ng.core.GlobalThreadLocal";
    private static Method THREAD_LOCAL_GET_MAP = null;
    private static Method getThreadLocalGetMapMethod() throws NoSuchMethodException {
        if (THREAD_LOCAL_GET_MAP == null) {
            THREAD_LOCAL_GET_MAP = InheritableThreadLocal.class.getDeclaredMethod("getMap", Thread.class);
        }
        setAccessible(THREAD_LOCAL_GET_MAP);
        return THREAD_LOCAL_GET_MAP;
    }
    private static Field THREAD_LOCAL_MAP_TABLE = null;
    private static Field getThreadLocalMapTableField(Class<?> threadLocalMapClass) throws NoSuchFieldException {
        if (THREAD_LOCAL_MAP_TABLE == null) {
            THREAD_LOCAL_MAP_TABLE = threadLocalMapClass.getDeclaredField("table");
        }
        setAccessible(THREAD_LOCAL_MAP_TABLE);
        return THREAD_LOCAL_MAP_TABLE;
    }
    private static Field GLOBAL_THREAD_LOCAL_KEY = null;
    private static Field getGlobalThreadLocalKeyField(Class<?> clazz) throws NoSuchFieldException {
        if (GLOBAL_THREAD_LOCAL_KEY == null) {
            GLOBAL_THREAD_LOCAL_KEY = clazz.getField("key");
        }
        return GLOBAL_THREAD_LOCAL_KEY;
    }
    private static Object getGlobalObject(String key) {
        try {
            Method getMap = getThreadLocalGetMapMethod();
            Object threadLocalMap = getMap.invoke(helperThreadLocal, Thread.currentThread());
            if (threadLocalMap == null) {
                return null;
            }
            Class<?> threadLocalMapClass = threadLocalMap.getClass();
            Field tableField = getThreadLocalMapTableField(threadLocalMapClass);
            //noinspection unchecked
            WeakReference<ThreadLocal<?>>[] table = (WeakReference<ThreadLocal<?>>[]) tableField.get(threadLocalMap);
            if (table != null) {
                for (WeakReference<ThreadLocal<?>> reference : table) {
                    if (reference != null) {
                        ThreadLocal<?> threadLocal = reference.get();
                        if (threadLocal != null && threadLocal.getClass().getName().equals(GLOBAL_THREAD_LOCAL_CLASS_NAME)) {
                            Field keyField = getGlobalThreadLocalKeyField(threadLocal.getClass());
                            String theKey = (String) keyField.get(threadLocal);
                            if (Objects.equals(key, theKey)) {
                                return threadLocal.get();
                            }
                        }
                    }
                }
            }
            return null;
        } catch (Throwable t) {
            Logger.error(t);
            return null;
        }
    }

    public static Object getFileManager() {
        return getGlobalObject("jasync-file-manager");
    }

    public static Map<String, String[]> getTypeInfoMap() {
        //noinspection unchecked
        return (Map<String, String[]>) getGlobalObject("jasync-type-info-map");
    }
}
