package io.github.vipcxj.jasync.ng.core;

import io.github.vipcxj.jasync.ng.utils.Logger;
import io.github.vipcxj.jasync.ng.utils.hack.Permit;

import javax.tools.JavaFileManager;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

public class GlobalThreadLocal<T> extends InheritableThreadLocal<T> {

    private final static String WRAPPED_JAVA_FILE_MANAGER_CLASS_NAME = "io.github.vipcxj.jasync.ng.core.WrappedJavaFileManager";

    public final String key;

    public GlobalThreadLocal(String key) {
        this.key = key;
        cleanOld();
    }

    @Override
    public void set(T value) {
        T old = get();
        if (old != value && old != null && old.getClass().getName().equals(WRAPPED_JAVA_FILE_MANAGER_CLASS_NAME)) {
            JavaFileManager fileManager = (JavaFileManager) old;
            try {
                fileManager.close();
            } catch (Throwable t) {
                Logger.warn("Unable to close the old file manager");
                Logger.warn(t);
            }
        }
        super.set(value);
    }

    private void cleanOld() {
        try {
            Method getMapMethod = InheritableThreadLocal.class.getDeclaredMethod("getMap", Thread.class);
            Permit.setAccessible(getMapMethod);
            Object map = getMapMethod.invoke(this, Thread.currentThread());
            if (map != null) {
                Field tableField = map.getClass().getDeclaredField("table");
                Permit.setAccessible(tableField);
                //noinspection unchecked
                WeakReference<ThreadLocal<?>>[] table = (WeakReference<ThreadLocal<?>>[]) tableField.get(map);
                if (table != null) {
                    for (WeakReference<ThreadLocal<?>> reference : table) {
                        if (reference != null) {
                            ThreadLocal<?> threadLocal = reference.get();
                            if (threadLocal != null) {
                                Class<?> threadLocalClass = threadLocal.getClass();
                                if (threadLocalClass.getName().equals(GlobalThreadLocal.class.getName())) {
                                    Field keyField = threadLocalClass.getField("key");
                                    String key = (String) keyField.get(threadLocal);
                                    if (Objects.equals(this.key, key)) {
                                        threadLocal.set(null);
                                        threadLocal.remove();
                                        Logger.info("Find stale GlobalThreadLocal with key: " + key + ". It's class is " + threadLocalClass + ".");
                                    }
                                }
                            }
                        }
                    }
                }

            }
        } catch (Throwable t) {
            Logger.error("Unable to clean the old GlobalThreadLocal with key: " + this.key + ".");
            Logger.error(t);
        }
    }
}
