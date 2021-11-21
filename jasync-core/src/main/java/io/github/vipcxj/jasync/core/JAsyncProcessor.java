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
import javax.lang.model.element.*;
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

    private static void installAgent() {
        try {
            Utils.addOpensFromJdkCompilerModule(AsyncProcessor.class, new String[] {
                    "com.sun.tools.javac.jvm",
                    "com.sun.tools.javac.main"
            });
            ByteBuddyAgent.install();
            ClassLoader classLoader = ClassLoader.getSystemClassLoader();
            TypePool typePool = TypePool.Default.of(classLoader);
            new ByteBuddy()
                    .redefine(typePool.describe("com.sun.tools.javac.jvm.ClassWriter").resolve(), ClassFileLocator.ForClassLoader.of(classLoader))
                    .visit(Advice.to(ClassWriterAdvice.class).on(ElementMatchers.named("writeClass").and(ElementMatchers.returns(JavaFileObject.class))))
                    .make().load(classLoader, ClassReloadingStrategy.fromInstalledAgent());
            System.out.println("modify ClassWriter");
/*            new ByteBuddy()
                    .redefine(typePool.describe("com.sun.tools.javac.main.JavaCompiler").resolve(), ClassFileLocator.ForClassLoader.of(classLoader))
                    .visit(Advice.to(JavacCompilerGenerateAdvice.class).on(ElementMatchers.named("generate")))
                    .make().load(classLoader, ClassReloadingStrategy.fromInstalledAgent());
            System.out.println("modify JavaCompiler");*/
            attachAsmJar();

/*            classLoader = JAsyncProcessor.class.getClassLoader();
            typePool = TypePool.Default.of(classLoader);
            TestClass testClass = new TestClass();
            System.out.println(testClass.helloWorld());
            new ByteBuddy()
                    .redefine(typePool.describe("io.github.vipcxj.jasync.core.JAsyncProcessor$TestClass").resolve(), ClassFileLocator.ForClassLoader.of(classLoader))
                    .visit(Advice.to(MyAdvice.class).on(ElementMatchers.named("helloWorld")))
                    .make().load(classLoader, ClassReloadingStrategy.fromInstalledAgent());
            System.out.println(testClass.helloWorld());*/
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

    private final Set<String> promiseTypes = new HashSet<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        collectTypes();
    }

    private void writePromiseTypes() {
        try {
            Path path = Files.createTempFile("promise-types", ".txt");
            Files.write(path, promiseTypes, StandardCharsets.UTF_8);
            File file = path.toFile();
            file.deleteOnExit();
            System.setProperty(PROMISE_TYPES_FILE_PROPERTY, file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    private void collectTypes() {
        try {
            JavaFileManager fileManager = getFileManager(processingEnv.getFiler());
            try {
                Map<String, TypeInfo> typeInfoMap = new HashMap<>();
                Iterable<JavaFileObject> fileObjects = fileManager.list(StandardLocation.CLASS_PATH, "", Collections.singleton(JavaFileObject.Kind.CLASS), true);
                for (JavaFileObject fileObject : fileObjects) {
                    try (InputStream is = fileObject.openInputStream()) {
                        ClassReader classReader = new ClassReader(is);
                        TypeScanner typeScanner = new TypeScanner(null);
                        classReader.accept(typeScanner, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
                        typeInfoMap.put(typeScanner.getName(), new TypeInfo(typeScanner.getSuperName(), typeScanner.getInterfaces()));
                    }
                }
                typeInfoMap.keySet().stream()
                        .filter(name -> isPromise(typeInfoMap, name))
                        .forEach(promiseTypes::add);
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

    private boolean isPromise(TypeElement typeElement) {
        if (typeElement.getQualifiedName().contentEquals(promiseTypeName)) {
            return true;
        }
        TypeMirror superClass = typeElement.getSuperclass();
        if (superClass != null) {
            Element element = processingEnv.getTypeUtils().asElement(superClass);
            if (element instanceof TypeElement) {
                TypeElement superTypeElement = (TypeElement) element;
                if (isPromise(superTypeElement)) {
                    return true;
                }
            }
        }
        for (TypeMirror anInterface : typeElement.getInterfaces()) {
            Element element = processingEnv.getTypeUtils().asElement(anInterface);
            if (element instanceof TypeElement) {
                TypeElement interfaceTypeElement = (TypeElement) element;
                if (isPromise(interfaceTypeElement)) {
                    return true;
                }
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
        ElementScanner8<Void, Void> elementScanner = new ElementScanner8<Void, Void>() {
            @Override
            public Void visitType(TypeElement e, Void unused) {
                ElementKind kind = e.getKind();
                if (kind == ElementKind.CLASS || kind == ElementKind.INTERFACE) {
                    if (isPromise(e)) {
                        Name binaryName = processingEnv.getElementUtils().getBinaryName(e);
                        promiseTypes.add(binaryName.toString());
                    }
                }
                return null;
            }
        };
        for (Element rootElement : roundEnv.getRootElements()) {
            if (rootElement instanceof TypeElement) {
                visitElement((TypeElement) rootElement, elementScanner);
            }
        }
        writePromiseTypes();
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

/*    static class JavacCompilerGenerateAdvice {

        @Advice.OnMethodEnter
        public static void enter(@Advice.FieldValue("procEnvImpl") Object procEnvImpl) {
            if (procEnvImpl != null) {
                System.out.println("find nonnull field procEnvImpl");
                try {
                    Class<?> transformerClass = Class.forName("io.github.vipcxj.jasync.asm.Utils");
                    Method transform = transformerClass.getMethod("enterCompile", Object.class);
                    transform.invoke(null, procEnvImpl);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }

        @Advice.OnMethodExit
        public static void exit() {
            try {
                Class<?> transformerClass = Class.forName("io.github.vipcxj.jasync.asm.Utils");
                Method transform = transformerClass.getMethod("exitCompile");
                transform.invoke(null);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }*/

}
