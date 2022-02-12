package io.github.vipcxj.jasync.core;

import com.google.auto.service.AutoService;
import io.github.vipcxj.jasync.core.asm.TypeInfo;
import io.github.vipcxj.jasync.core.asm.TypeScanner;
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
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
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
import java.nio.file.StandardOpenOption;
import java.security.cert.Certificate;
import java.util.*;
import java.util.jar.JarFile;

@SupportedAnnotationTypes("*")
@AutoService(Processor.class)
public class JAsyncProcessor extends AbstractProcessor {

    static {
        System.out.println("install agent.");
        installAgent();
    }

    private static final String PROMISE_TYPES_FILE_PROPERTY = "jasync_promise_types_file";
    private static final String FULL_TYPES_FILE_PROPERTY = "jasync_full_types_file";

    private static void installAgent() {
        try {
            Utils.addOpensFromJdkCompilerModule(JAsyncProcessor.class, new String[] {
                    "com.sun.tools.javac.processing",
                    "com.sun.tools.javac.jvm",
                    "com.sun.tools.javac.main"
            });
            ByteBuddyAgent.install();
            ClassLoader classLoader = ClassLoader.getSystemClassLoader();
            TypePool typePool = TypePool.Default.of(classLoader);
            Throwable error = null;
            boolean success = false;
            try {
                new ByteBuddy()
                        .redefine(typePool.describe("com.sun.tools.javac.jvm.ClassWriter").resolve(), ClassFileLocator.ForClassLoader.of(classLoader))
                        .visit(Advice.to(ClassWriterAdvice.class).on(ElementMatchers.named("writeClass").and(ElementMatchers.returns(JavaFileObject.class))))
                        .make().load(classLoader, ClassReloadingStrategy.fromInstalledAgent());
                success = true;
                System.out.println("Successful to modify Javac ClassWriter");
            } catch (Throwable t) {
                error = t;
                error.printStackTrace();
                System.out.println("Fail to modify Javac ClassWriter");
            }
            try {
                String classToHack = "org.eclipse.jdt.internal.compiler.ClassFile";
                Utils.addOpens(JAsyncProcessor.class, "java.base", new String[] {"java.lang"});
                Certificate[] certs = Utils.unsecureClassloader(classLoader, classToHack);
                new ByteBuddy()
                        .redefine(typePool.describe(classToHack).resolve(), ClassFileLocator.ForClassLoader.of(classLoader))
                        .visit(Advice.to(ClassFileAdvice.class).on(ElementMatchers.named("getBytes").and(ElementMatchers.takesArguments(0)).and(ElementMatchers.returns(byte[].class))))
                        .make().load(classLoader, ClassReloadingStrategy.fromInstalledAgent());
                Utils.secureClassloader(classLoader, classToHack, certs);
                success = true;
                System.out.println("Successful to modify ECJ ClassFile");
            } catch (Throwable t) {
                System.out.println("Fail to modify ECJ ClassFile");
                error = t;
                error.printStackTrace();
            }
            if (success) {
                attachAsmJar();
            } else {
                error.printStackTrace();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
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
        collectTypes();
    }

    public static Object tryGetProxyDelegateToField(Object instance) {
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
                writeFile(0, promiseTypes.size(), 0, fullTypes.size());
            } catch (IOException e) {
                throw new IllegalStateException("Unable to collect types.", e);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static final String promiseTypeName = JPromise2.class.getName();
    private boolean isPromise(Map<String, TypeInfo> typeInfoMap, String name) {
        if (promiseTypeName.equals(name)) {
            return true;
        }
        TypeInfo typeInfo = typeInfoMap.get(name);
        if (typeInfo == null) {
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
        try {
            int curPromiseSize = promiseTypes.size();
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
            int afterPromiseSize = promiseTypes.size();
            int afterFullSize = fullTypes.size();
            writeFile(curPromiseSize, afterPromiseSize, curFullSize, afterFullSize);
        } catch (Throwable t) {
            t.printStackTrace();
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
                    e.printStackTrace();
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
        public static void exit(@Advice.Enter boolean init, @Advice.FieldValue(value = "bytes", readOnly = false) byte[] bytes) {
            if (init && bytes != null) {
                try {
                    Class<?> transformerClass = Class.forName("io.github.vipcxj.jasync.asm.JAsyncTransformer");
                    Method transform = transformerClass.getMethod("transform", byte[].class);
                    //noinspection PrimitiveArrayArgumentToVarargsMethod
                    bytes = (byte[]) transform.invoke(null, bytes);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
