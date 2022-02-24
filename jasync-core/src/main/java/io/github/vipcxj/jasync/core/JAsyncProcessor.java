package io.github.vipcxj.jasync.core;

import com.google.auto.service.AutoService;
import io.github.vipcxj.jasync.core.asm.ECJClassWriterEnhancer;
import io.github.vipcxj.jasync.core.asm.TypeInfo;
import io.github.vipcxj.jasync.spec.JPromise2;
import io.github.vipcxj.jasync.utils.hack.Utils;
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
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementScanner8;
import javax.tools.JavaFileObject;
import java.io.*;
import java.lang.reflect.Method;
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
            classLoader.loadClass(className);
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
    private final Set<String> promiseTypes = new HashSet<>();
    private final List<String> fullTypes = new ArrayList<>();
    private Path fullTypesPath;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        Logger.info("install agent.");
        installAgent(processingEnv.getClass().getClassLoader());
        Logger.info(Paths.get("").toAbsolutePath().toString());
        Logger.info("init jasync processor.");
        Logger.info("processingEnv type: " + processingEnv.getClass());
        createFullTypesFile();
    }

    private void createFullTypesFile() {
        try {
            fullTypesPath = Files.createTempFile("full-types", ".txt");
            System.setProperty(FULL_TYPES_FILE_PROPERTY, fullTypesPath.toString());
            deleteOnExit(fullTypesPath);
        } catch (Throwable t) {
            Logger.error("Unable to create the full-types file.");
            Logger.error(t);
        }
    }

/*    public static Object tryGetProxyDelegateToField(Object instance) {
        try {
            InvocationHandler handler = Proxy.getInvocationHandler(instance);
            return Permit.getField(handler.getClass(), "val$delegateTo").get(handler);
        } catch (Exception e) {
            return null;
        }
    }

    private JavaFileManager getFileManager(Filer filer) {
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
                        throw new IllegalStateException("Unable to get the java file manager from the filer object. The filer object type is " + filerClass + ".", e3);
                    }
                }
            }
        }
        fileManagerField.setAccessible(true);
        try {
            Object fileManagerObject = fileManagerField.get(filer);
            if (fileManagerObject instanceof JavaFileManager) {
                Logger.info("Find the file manager: " + fileManagerObject.getClass());
                return (JavaFileManager) fileManagerObject;
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
    }*/

    private void deleteOnExit(Path path) {
        try {
            path.toFile().deleteOnExit();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void writeFile(int fullFrom, int fullTo) {
        try {
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

    private static final String promiseTypeName = JPromise2.class.getName();

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

    private void visitElement(TypeElement element, ElementVisitor<Void, Void> visitor) {
        element.accept(visitor, null);
        for (Element enclosedElement : element.getEnclosedElements()) {
            if (enclosedElement instanceof TypeElement) {
                visitElement((TypeElement) enclosedElement, visitor);
            }
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Logger.info("processing...");
        try {
            int curFullSize = fullTypes.size();
            ElementScanner8<Void, Void> elementScanner = new ElementScanner8<Void, Void>() {
                @Override
                public Void visitType(TypeElement e, Void unused) {
                    collectTypes(e);
                    return null;
                }
            };
            for (Element rootElement : roundEnv.getRootElements()) {
                if (rootElement instanceof TypeElement) {
                    visitElement((TypeElement) rootElement, elementScanner);
                }
            }
            int afterFullSize = fullTypes.size();
            writeFile(curFullSize, afterFullSize);
        } catch (Throwable t) {
            Logger.error(t);
        }
        return true;
    }

    static class ClassWriterAdvice {

        @Advice.OnMethodExit
        public static void exit(@Advice.FieldValue("fileManager") Object fileManager, @Advice.Return Object fileObject) {
            if (fileObject != null) {
                try {
                    Class<?> transformerClass = Class.forName("io.github.vipcxj.jasync.asm.Transformer");
                    Method transform = transformerClass.getMethod("transform", Object.class, Object.class);
                    transform.invoke(null, fileManager, fileObject);
                } catch (Throwable e) {
                    Logger.error(e);
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
                    Logger.error(e);
                }
            }
        }
    }

}
