package io.github.vipcxj.jasync.core.jdt;

import io.github.vipcxj.jasync.core.ReflectHelper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Vector;

public class ClassLoaderHelper {

    private static String getPackage(String binaryName) {
        int i = binaryName.lastIndexOf(".");
        if (i == -1) {
            return  "";
        } else {
            return binaryName.substring(0, i);
        }
    }

    private static Method BUNDLE_LOAD_CLASS;

    private static Class<?> loadEClass(Object bundle, String name) {
        try {
            if (BUNDLE_LOAD_CLASS == null) {
                BUNDLE_LOAD_CLASS = bundle.getClass().getMethod("loadClass", String.class);
            }
            return (Class<?>) BUNDLE_LOAD_CLASS.invoke(bundle, name);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Method BUNDLE_CONTEXT_GET_BUNDLES;
    private static Class<?> loadEClassFromContext(Object bundleContext, String name) {
        try {
            if (BUNDLE_CONTEXT_GET_BUNDLES == null) {
                BUNDLE_CONTEXT_GET_BUNDLES = bundleContext.getClass().getMethod("getBundles");
            }
            Object[] bundles = (Object[]) BUNDLE_CONTEXT_GET_BUNDLES.invoke(bundleContext);
            for (Object bundle : bundles) {
                Class<?> eClass = loadEClass(bundle, name);
                if (eClass != null) {
                    // Logger.info("Load osgi class " + name + " by bundle " + bundle);
                    return eClass;
                }
            }
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object getBundleContext(Class<?> fromClass) {
        ClassLoader classLoader = fromClass.getClassLoader();
        try {
            Class<?> utilClass = classLoader.loadClass("org.osgi.framework.FrameworkUtil");
            Method getBundle = utilClass.getMethod("getBundle", Class.class);
            Object bundle = getBundle.invoke(null, fromClass);
            Class<?> bundleClass = bundle.getClass();
            Method getBundleContext = bundleClass.getMethod("getBundleContext");
            return getBundleContext.invoke(bundle);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Class<?> loadClass(String name, Class<?> helperClass) throws ClassNotFoundException {
        Object bundleContext = getBundleContext(helperClass);
        ClassLoader classLoader = ReflectHelper.getClassLoader(Thread.currentThread().getContextClassLoader(), helperClass.getClassLoader());
        return new MixClassLoader(
                classLoader,
                getPackage(name),
                bundleContext
        ).loadClass(name);
    }

    static class MixClassLoader extends ClassLoader {

        private final String packageName;
        private final Object bundleContext;

        MixClassLoader(ClassLoader parent, String targetName, Object bundleContext) {
            super(parent);
            this.packageName = targetName;
            this.bundleContext = bundleContext;
//            Logger.info("Create MixClassLoader with " +
//                    "parent class loader: " + parent +
//                    ", targetName: " + targetName +
//                    ", bundleContext: " + bundleContext);
        }

        private boolean isTarget(String name) {
            return Objects.equals(packageName, ClassLoaderHelper.getPackage(name));
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                // First, check if the class has already been loaded
                Class<?> c = findLoadedClass(name);
                if (c == null) {
                    if (isTarget(name)) {
                        c = findClass(name);
                    } else {
                        c = bundleContext != null ? loadEClassFromContext(bundleContext, name) : null;
                        if (c == null) {
                            return getParent().loadClass(name);
                        } else {
                            return c;
                        }
                    }
                }
                if (resolve) {
                    resolveClass(c);
                }
                return c;
            }
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            String path = name.replace('.', '/') + ".class";
            URL url = findResource(path);
            if (url == null) {
                throw new ClassNotFoundException(name); }
            ByteBuffer byteCode;
            try {
                byteCode = loadResource(url); }
            catch (IOException e) {
                throw new ClassNotFoundException(name, e); }
            return defineClass(name, byteCode, null);
        }

        private ByteBuffer loadResource (URL url) throws IOException {
            try (InputStream stream = url.openStream()) {
                int initialBufferCapacity = Math.min(0x40000, stream.available() + 1);
                if (initialBufferCapacity <= 2) {
                    initialBufferCapacity = 0x10000;
                } else {
                    initialBufferCapacity = Math.max(initialBufferCapacity, 0x200);
                }
                ByteBuffer buf = ByteBuffer.allocate(initialBufferCapacity);
                while (true) {
                    if (!buf.hasRemaining()) {
                        ByteBuffer newBuf = ByteBuffer.allocate(2 * buf.capacity());
                        buf.flip();
                        newBuf.put(buf);
                        buf = newBuf;
                    }
                    int len = stream.read(buf.array(), buf.position(), buf.remaining());
                    if (len <= 0) {
                        break;
                    }
                    buf.position(buf.position() + len);
                }
                buf.flip();
                return buf;
            }
        }

        private ClassLoader getCurrentClassLoader() {
            ClassLoader classLoader = ClassLoaderHelper.class.getClassLoader();
            return classLoader != null ? classLoader : ClassLoader.getSystemClassLoader();
        }

        protected URL findResource (String name) {
            return getCurrentClassLoader().getResource(name);
        }

        protected Enumeration<URL> findResources (String name) throws IOException {
            Vector<URL> vector = new Vector<>();
            Enumeration<URL> enumeration = getCurrentClassLoader().getResources(name);
            while (enumeration.hasMoreElements()) {
                vector.add(enumeration.nextElement());
            }
            return vector.elements();
        }
    }
}
