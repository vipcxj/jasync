package io.github.vipcxj.jasync.ng.core;

import com.google.auto.service.AutoService;
import io.github.vipcxj.jasync.ng.core.asm.ECJClassWriterEnhancer;
import io.github.vipcxj.jasync.ng.core.jdt.EcjTypeCollector;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.utils.Logger;
import io.github.vipcxj.jasync.ng.utils.TypeInfoHelper;
import io.github.vipcxj.jasync.ng.utils.hack.Permit;
import io.github.vipcxj.jasync.ng.utils.hack.Utils;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementScanner8;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Paths;
import java.security.cert.Certificate;
import java.util.*;
import java.util.jar.JarFile;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class JAsyncProcessor extends AbstractProcessor {

    private static volatile boolean agent = false;
    private static final GlobalThreadLocal<JavaFileManager> fileManagerThreadLocal = new GlobalThreadLocal<>("jasync-file-manager");
    private static final GlobalThreadLocal<Map<String, String[]>> typeInfoMapThreadLocal = new GlobalThreadLocal<>("jasync-type-info-map");

    private synchronized static void installAgent(ClassLoader classLoader) {
        if (agent) {
            return;
        }
        try {
            Utils.addOpensFromJdkCompilerModule(JAsyncProcessor.class, new String[] {
                    "com.sun.tools.javac.processing",
                    "com.sun.tools.javac.jvm",
                    "com.sun.tools.javac.main"
            });
            ByteBuddyAgent.install();
            TypePool typePool;
            boolean success = false;
            try {
                String classToHack = "com.sun.tools.javac.jvm.ClassWriter";
                ClassLoader currentClassLoader = findClassLoader(classLoader, classToHack);
                if (currentClassLoader != null) {
                    typePool = TypePool.Default.of(currentClassLoader);
                    new ByteBuddy()
                            .redefine(typePool.describe("com.sun.tools.javac.jvm.ClassWriter").resolve(), ClassFileLocator.ForClassLoader.of(classLoader))
                            .visit(Advice.to(ClassWriterAdvice.class).on(ElementMatchers.named("writeClass").and(ElementMatchers.returns(JavaFileObject.class))))
                            .make().load(currentClassLoader, ClassReloadingStrategy.fromInstalledAgent());
                    success = true;
                    Logger.info("Successful to modify Javac ClassWriter");
                } else {
                    Logger.info("Fail to modify Javac ClassWriter");
                }
            } catch (Throwable t) {
                t.printStackTrace();
               Logger.info("Fail to modify Javac ClassWriter");
            }
            try {
                String classToHack = "org.eclipse.jdt.internal.compiler.ClassFile";
                ClassLoader currentClassLoader = findClassLoader(classLoader, classToHack);
                if (currentClassLoader != null) {
                    typePool = TypePool.Default.of(currentClassLoader);
                    Utils.addOpens(JAsyncProcessor.class, "java.base", new String[] {"java.lang"});
                    Certificate[] certs = Utils.unsecureClassloader(currentClassLoader, classToHack);
                    new ByteBuddy()
                            .redefine(typePool.describe(classToHack).resolve(), ClassFileLocator.ForClassLoader.of(classLoader))
                            .visit(Advice.to(ClassFileAdvice.class).on(ElementMatchers.named("getBytes").and(ElementMatchers.takesArguments(0)).and(ElementMatchers.returns(byte[].class))))
                            .make().load(currentClassLoader, ClassReloadingStrategy.fromInstalledAgent());
                    Utils.secureClassloader(currentClassLoader, classToHack, certs);
                    Logger.info("Successful to modify ECJ ClassFile");
                    try {
                        classToHack = "org.eclipse.jdt.internal.compiler.tool.EclipseCompilerImpl";
                        currentClassLoader = findClassLoader(classLoader, classToHack);
                        if (currentClassLoader != null) {
                            typePool = TypePool.Default.of(currentClassLoader);
                            certs = Utils.unsecureClassloader(currentClassLoader, classToHack);
                            new ByteBuddy()
                                    .redefine(typePool.describe(classToHack).resolve(), ClassFileLocator.ForClassLoader.of(classLoader))
                                    .visit(new ECJClassWriterEnhancer("outputClassFiles"))
                                    .make().load(currentClassLoader, ClassReloadingStrategy.fromInstalledAgent());
                            Utils.secureClassloader(currentClassLoader, classToHack, certs);
                            success = true;
                            Logger.info("Successful to modify ECJ EclipseCompilerImpl");
                        } else {
                            Logger.warn("Fail to modify ECJ EclipseCompilerImpl");
                        }
                    } catch (Throwable t) {
                        Logger.warn("Fail to modify ECJ EclipseCompilerImpl");
                        Logger.error(t);
                    }
                    try {
                        classToHack = "org.eclipse.jdt.internal.compiler.util.Util";
                        currentClassLoader = findClassLoader(classLoader, classToHack);
                        if (currentClassLoader != null) {
                            typePool = TypePool.Default.of(currentClassLoader);
                            certs = Utils.unsecureClassloader(currentClassLoader, classToHack);
                            new ByteBuddy()
                                    .redefine(typePool.describe(classToHack).resolve(), ClassFileLocator.ForClassLoader.of(classLoader))
                                    .visit(new ECJClassWriterEnhancer("writeToDisk"))
                                    .make().load(currentClassLoader, ClassReloadingStrategy.fromInstalledAgent());
                            Utils.secureClassloader(currentClassLoader, classToHack, certs);
                            success = true;
                            Logger.info("Successful to modify ECJ Util");
                        } else {
                            Logger.warn("Fail to modify ECJ Util");
                        }
                    } catch (Throwable t) {
                        Logger.warn("Fail to modify ECJ Util");
                        Logger.error(t);
                    }
                }
            } catch (Throwable t) {
                Logger.info("Fail to modify ECJ ClassFile");
                Logger.error(t);
                t.printStackTrace();
            }
            if (success) {
                attachAsmJar();
            }
        } catch (Throwable t) {
            Logger.error(t);
        }
        agent = true;
    }

    private static ClassLoader findClassLoader(ClassLoader classLoader, String className) {
        try {
            if (classLoader == null) {
                classLoader = JAsyncProcessor.class.getClassLoader();
            }
            if (classLoader == null) {
                classLoader = ClassLoader.getSystemClassLoader();
            }
            Class<?> theClass = classLoader.loadClass(className);
            classLoader = theClass.getClassLoader();
            if (classLoader == null) {
                classLoader = ClassLoader.getSystemClassLoader();
            }
            Logger.info("Success to load the class " + className + ", so use the class loader " + classLoader);
            return classLoader;
        } catch (ClassNotFoundException ignored) {}
        Logger.info("Unable to find the class loader of " + className + ".");
        return null;
    }

    private static void attachAsmJar() {
        try {
            File asmJar = File.createTempFile("asm", ".jar");
            asmJar.deleteOnExit();
            InputStream asmIs = JAsyncProcessor.class.getResourceAsStream("/asm.jar");
            if (asmIs == null) {
                throw new IllegalStateException("The asm jar is missing.");
            }
            try (InputStream is = new BufferedInputStream(asmIs)) {
                try (OutputStream os = new FileOutputStream(asmJar)){
                    byte[] buffer = new byte[64 * 1024];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                    }
                    os.flush();
                }
            }
            ByteBuddyAgent.getInstrumentation().appendToBootstrapClassLoaderSearch(new JarFile(asmJar));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to attach the asm jar.", e);
        }
    }

    private final List<String> promiseTypes = new ArrayList<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        Logger.info(processingEnv.getClass().toString());
        Logger.info("Install agent.");
        installAgent(processingEnv.getClass().getClassLoader());
        Logger.info("Get JavaFileManager.");
        getFileManager(processingEnv.getFiler());
        Logger.info(Paths.get("").toAbsolutePath().toString());
    }

    public static Object tryGetProxyDelegateToField(Object instance) {
        try {
            InvocationHandler handler = Proxy.getInvocationHandler(instance);
            return Permit.getField(handler.getClass(), "val$delegateTo").get(handler);
        } catch (Exception e) {
            return null;
        }
    }

    private static Object unwrapProxy(Object maybeProxy) {
        Object delegate = Utils.tryGetProxyDelegateToField(maybeProxy);
        Object unwrapped = maybeProxy;
        while (delegate != null) {
            unwrapped = delegate;
            delegate = Utils.tryGetProxyDelegateToField(delegate);
        }
        return unwrapped;
    }

    private static void getFileManager(Filer filer) {
        try {
            JavaFileManager fileManager = _getFileManager(filer);
            Logger.info("Find JavaFileManager " + fileManager.getClass());
            fileManagerThreadLocal.set(fileManager);
        } catch (Throwable t) {
            Logger.error("Unable to get the file manager.");
            Logger.error(t);
        }
    }

    private static JavaFileManager _getFileManager(Filer filer) {
        Class<? extends Filer> filerClass = filer.getClass();
        Field fileManagerField;
        try {
            fileManagerField = filerClass.getDeclaredField("fileManager");
        } catch (NoSuchFieldException e0) {
            try {
                fileManagerField = filerClass.getDeclaredField("_fileManager");
            } catch (NoSuchFieldException e1) {
                try {
                    fileManagerField = filerClass.getDeclaredField("javaFileManager");
                } catch (NoSuchFieldException e2) {
                    try {
                        fileManagerField = filerClass.getDeclaredField("_javaFileManager");
                    } catch (NoSuchFieldException e3) {
                        Object realFiler = tryGetProxyDelegateToField(filer);
                        if (realFiler instanceof Filer) {
                            return _getFileManager((Filer) realFiler);
                        }
                        JavaFileManager fileManager = EcjTypeCollector.getFileManager(filer);
                        if (fileManager != null) {
                            return fileManager;
                        }
                        throw new IllegalStateException("Unable to get the java file manager from the filer object. The filer object type is " + filerClass + ".", e3);
                    }
                }
            }
        }
        fileManagerField.setAccessible(true);
        try {
            Object fileManagerObject = fileManagerField.get(filer);
            if (fileManagerObject instanceof JavaFileManager) {
                return (JavaFileManager) unwrapProxy(fileManagerObject);
            } else {
                String errorMessage = "Unable to get the java file manager from the filer object.";
                if (fileManagerObject != null) {
                    throw new IllegalStateException(errorMessage + " The file manager object type is " + fileManagerObject.getClass() + ".");
                } else {
                    throw new IllegalStateException(errorMessage + " The file manager object is null.");
                }
            }
        } catch (IllegalAccessException e) {
            String errorMessage = "Unable to get the java file manager from the filer object.";
            throw new IllegalStateException(errorMessage + " " + e.getMessage(), e);
        }
    }

    private static final String promiseTypeName = JPromise.class.getName();

    private String getBinaryName(TypeElement typeElement) {
        return processingEnv.getElementUtils().getBinaryName(typeElement).toString();
    }

    private TypeElement asElement(TypeMirror typeMirror) {
        return (TypeElement) processingEnv.getTypeUtils().asElement(typeMirror);
    }

    private String getBinaryName(TypeMirror typeMirror) {
        TypeElement typeElement = asElement(typeMirror);
        return getBinaryName(typeElement);
    }

    private static final String objectName = "java.lang.Object";

    private Map<String, String[]> getTypeInfoMap() {
        Map<String, String[]> typeInfoMap = typeInfoMapThreadLocal.get();
        if (typeInfoMap == null) {
            typeInfoMap = new HashMap<>();
            typeInfoMapThreadLocal.set(typeInfoMap);
        }
        return typeInfoMap;
    }

    private boolean collectTypes(TypeElement typeElement) {
        String name = getBinaryName(typeElement);
        Map<String, String[]> typeInfoMap = getTypeInfoMap();
        if (typeInfoMap.containsKey(name)) {
            return promiseTypes.contains(name);
        }
        if (typeElement.getQualifiedName().contentEquals(objectName)) {
            String[] objectTypeInfo = TypeInfoHelper.typeInfo(null);
            typeInfoMap.put(objectName, objectTypeInfo);
            return false;
        }
        TypeMirror superClass = typeElement.getSuperclass();
        String superName;
        if (superClass != null && superClass.getKind() != TypeKind.NONE) {
            superName = getBinaryName(superClass);
        } else {
            superName = objectName;
        }
        String[] interfaces = new String[typeElement.getInterfaces().size()];
        int i = 0;
        for (TypeMirror anInterface : typeElement.getInterfaces()) {
            interfaces[i++] = getBinaryName(anInterface);
        }
        String[] typeInfo = TypeInfoHelper.typeInfo(superName, interfaces);
        typeInfoMap.put(name, typeInfo);
        if (name.contentEquals(promiseTypeName)) {
            promiseTypes.add(name);
            return true;
        }
        if (!superName.equals(objectName)) {
            if(collectTypes(asElement(superClass))) {
                promiseTypes.add(name);
                return true;
            }
        }
        for (TypeMirror anInterface : typeElement.getInterfaces()) {
            boolean isPromise = collectTypes(asElement(anInterface));
            if (isPromise) {
                promiseTypes.add(name);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Logger.info("processing...");
        try {
            int curPromiseSize = promiseTypes.size();
            ElementScanner8<Void, Void> elementScanner = new ElementScanner8<Void, Void>() {

                @Override
                public Void scan(Element e, Void unused) {
                    if (e instanceof TypeElement) {
                        collectTypes((TypeElement) e);
                    }
                    return super.scan(e, unused);
                }
            };
            for (Element rootElement : roundEnv.getRootElements()) {
                if (rootElement instanceof TypeElement) {
                    elementScanner.scan(rootElement);
                }
            }
            int afterPromiseSize = promiseTypes.size();
            Logger.info("promise types: " + curPromiseSize + " -> " + afterPromiseSize + ".");
        } catch (Throwable t) {
            Logger.error(t);
        }
        return true;
    }

    static class ClassWriterAdvice {

        @Advice.OnMethodExit
        public static void exit(@Advice.Return Object fileObject) {
            if (fileObject != null) {
                try {
                    Class<?> transformerClass = Class.forName("io.github.vipcxj.jasync.ng.asm.Transformer");
                    Method transform = transformerClass.getMethod("transform", Object.class);
                    transform.invoke(null, fileObject);
                } catch (Throwable e) {
                    try {
                        Class<?> loggerClass = Class.forName("io.github.vipcxj.jasync.ng.asm.Logger");
                        Method errorMethod = loggerClass.getMethod("error", Throwable.class);
                        errorMethod.invoke(null, e);
                    } catch (Throwable t) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    static class ClassFileAdvice {

        @Advice.OnMethodEnter
        public static boolean enter(@Advice.FieldValue("bytes") Object bytes) {
            return bytes == null;
        }

        @SuppressWarnings("UnusedAssignment")
        @Advice.OnMethodExit
        public static void exit(@Advice.Enter boolean init, @Advice.FieldValue(value = "bytes", readOnly = false) byte[] bytes, @Advice.Return(readOnly = false) byte[] out) {
            if (init && bytes != null) {
                try {
                    Class<?> transformerClass = Class.forName("io.github.vipcxj.jasync.ng.asm.JAsyncTransformer");
                    Method transform = transformerClass.getMethod("transform", byte[].class);
                    //noinspection PrimitiveArrayArgumentToVarargsMethod
                    bytes = (byte[]) transform.invoke(null, bytes);
                    out = bytes;
                } catch (Throwable e) {
                    try {
                        Class<?> loggerClass = Class.forName("io.github.vipcxj.jasync.ng.asm.Logger");
                        Method errorMethod = loggerClass.getMethod("error", Throwable.class);
                        errorMethod.invoke(null, e);
                    } catch (Throwable t) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}
