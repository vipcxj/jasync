package io.github.vipcxj.jasync.core;

import com.google.auto.service.AutoService;
import io.github.vipcxj.jasync.core.asm.ECJClassWriterEnhancer;
import io.github.vipcxj.jasync.core.asm.TypeInfo;
import io.github.vipcxj.jasync.core.asm.TypeScanner;
import io.github.vipcxj.jasync.core.jdt.EcjTypeCollector;
import io.github.vipcxj.jasync.spec.JPromise2;
import io.github.vipcxj.jasync.utils.hack.Permit;
import io.github.vipcxj.jasync.utils.hack.Utils;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import org.objectweb.asm.ClassReader;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementScanner8;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.cert.Certificate;
import java.util.*;
import java.util.jar.JarFile;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class JAsyncProcessor extends AbstractProcessor {

    private static final String PROMISE_TYPES_FILE_PROPERTY = "jasync_promise_types_file";
    private static final String FULL_TYPES_FILE_PROPERTY = "jasync_full_types_file";
    private static volatile boolean agent = false;

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

    private final Map<String, TypeInfo> typeInfoMap = new HashMap<>();
    private final List<String> promiseTypes = new ArrayList<>();
    private final List<String> fullTypes = new ArrayList<>();
    private Path promiseTypesPath;
    private Path fullTypesPath;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        Logger.info(processingEnv.getClass().toString());
        Logger.info("install agent.");
        installAgent(processingEnv.getClass().getClassLoader());
        Logger.info("collect types.");
        collectTypes();
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

    private static JavaFileManager getFileManager(Filer filer) {
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
                            return getFileManager((Filer) realFiler);
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

    private void deleteOnExit(Path path) {
        try {
            path.toFile().deleteOnExit();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void writeFile(int promiseFrom, int promiseTo, int fullFrom, int fullTo) {
        try {
            if (promiseTo > promiseFrom) {
                Files.write(
                        promiseTypesPath,
                        promiseTypes.subList(promiseFrom, promiseTo),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.APPEND
                );
            }
            if (fullTo > fullFrom) {
                Files.write(
                        fullTypesPath,
                        fullTypes.subList(fullFrom, fullTo),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.APPEND
                );
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void collectTypes() {
        try {
            JavaFileManager fileManager = getFileManager(processingEnv.getFiler());
            Logger.info("Find the file manager: " + fileManager.getClass());
            try {
                promiseTypesPath = Files.createTempFile("promise-types", ".txt");
                System.setProperty(PROMISE_TYPES_FILE_PROPERTY, promiseTypesPath.toString());
                deleteOnExit(promiseTypesPath);
                fullTypesPath = Files.createTempFile("full-types", ".txt");
                System.setProperty(FULL_TYPES_FILE_PROPERTY, fullTypesPath.toString());
                deleteOnExit(fullTypesPath);
                typeInfoMap.clear();
                Iterable<JavaFileObject> fileObjects = fileManager.list(StandardLocation.CLASS_PATH, "", Collections.singleton(JavaFileObject.Kind.CLASS), true);
                for (JavaFileObject fileObject : fileObjects) {
                    // Logger.info("Class in class path: " + fileObject.toUri());
                    try (InputStream is = fileObject.openInputStream()) {
                        ClassReader classReader = new ClassReader(is);
                        TypeScanner typeScanner = new TypeScanner(null);
                        classReader.accept(typeScanner, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
                        typeInfoMap.put(typeScanner.getName(), new TypeInfo(typeScanner.getSuperName(), typeScanner.getInterfaces()));
                    }
                }
                for (Map.Entry<String, TypeInfo> entry : typeInfoMap.entrySet()) {
                    String name = entry.getKey();
                    TypeInfo typeInfo = entry.getValue();
                    if (isPromise(typeInfoMap, name)) {
                        promiseTypes.add(name);
                    }
                    fullTypes.add(typeInfo.print(name));
                }
                Logger.info("promise types: 0 -> " + promiseTypes.size() + ".");
                Logger.info("full types: 0 -> " + fullTypes.size() + ".");
                writeFile(0, promiseTypes.size(), 0, fullTypes.size());
            } catch (IOException e) {
                throw new IllegalStateException("Unable to collect types.", e);
            }
        } catch (Throwable t) {
            Logger.error(t);
        }
    }

    private static final String promiseTypeName = JPromise2.class.getName();
    private boolean isPromise(Map<String, TypeInfo> typeInfoMap, String name) {
        if (promiseTypeName.equals(name)) {
            return true;
        }
        TypeInfo typeInfo = typeInfoMap.get(name);
        if (typeInfo == null) {
            // Maybe a promise, but we can't get type element from binary name. So we can not decide whether it is a promise.
            // But in the asm jar, we will check it using the type info and platform class loader again.
            return false;
        }
        String superName = typeInfo.getSuperName();
        if (superName != null && isPromise(typeInfoMap, superName)) {
            return true;
        }
        String[] interfaces = typeInfo.getInterfaces();
        for (String anInterface : interfaces) {
            if (isPromise(typeInfoMap, anInterface)) {
                return true;
            }
        }
        return false;
    }

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

    private boolean collectTypes(TypeElement typeElement) {
        String name = getBinaryName(typeElement);
        if (typeInfoMap.containsKey(name)) {
            return promiseTypes.contains(name);
        }
        if (typeElement.getQualifiedName().contentEquals(objectName)) {
            TypeInfo objectTypeInfo = new TypeInfo("", new String[]{});
            typeInfoMap.put(objectName, objectTypeInfo);
            fullTypes.add(objectTypeInfo.print(objectName));
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
        TypeInfo typeInfo = new TypeInfo(superName, interfaces);
        typeInfoMap.put(name, typeInfo);
        fullTypes.add(typeInfo.print(name));
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
            int curFullSize = fullTypes.size();
            ElementScanner8<Void, Void> elementScanner = new ElementScanner8<Void, Void>() {

                @Override
                public Void scan(Element e, Void unused) {
                    Logger.info("Find " + e.getKind() + " element: " + e.getSimpleName() + " with type " + e.asType());
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
            int afterFullSize = fullTypes.size();
            Logger.info("promise types: " + curPromiseSize + " -> " + afterPromiseSize + ".");
            Logger.info("full types: " + curFullSize + " -> " + afterFullSize + ".");
            writeFile(curPromiseSize, afterPromiseSize, curFullSize, afterFullSize);
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
                    Class<?> transformerClass = Class.forName("io.github.vipcxj.jasync.asm.Transformer");
                    Method transform = transformerClass.getMethod("transform", Object.class);
                    transform.invoke(null, fileObject);
                } catch (Throwable e) {
                    try {
                        Class<?> loggerClass = Class.forName("io.github.vipcxj.jasync.asm.Logger");
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
                    Class<?> transformerClass = Class.forName("io.github.vipcxj.jasync.asm.JAsyncTransformer");
                    Method transform = transformerClass.getMethod("transform", byte[].class);
                    //noinspection PrimitiveArrayArgumentToVarargsMethod
                    bytes = (byte[]) transform.invoke(null, bytes);
                    out = bytes;
                } catch (Throwable e) {
                    try {
                        Class<?> loggerClass = Class.forName("io.github.vipcxj.jasync.asm.Logger");
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
