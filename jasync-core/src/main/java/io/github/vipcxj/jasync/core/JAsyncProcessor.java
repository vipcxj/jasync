package io.github.vipcxj.jasync.core;

import com.google.auto.service.AutoService;
import io.github.vipcxj.jasync.utils.hack.Utils;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;

import javax.annotation.processing.*;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.*;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.jar.JarFile;

@SupportedAnnotationTypes("io.github.vipcxj.jasync.spec.annotations.Async")
@AutoService(Processor.class)
public class JAsyncProcessor extends AbstractProcessor {

    static {
        System.out.println("install agent.");
        installAgent();
    }

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
            new ByteBuddy()
                    .redefine(typePool.describe("com.sun.tools.javac.main.JavaCompiler").resolve(), ClassFileLocator.ForClassLoader.of(classLoader))
                    .visit(Advice.to(JavacCompilerGenerateAdvice.class).on(ElementMatchers.named("generate")))
                    .make().load(classLoader, ClassReloadingStrategy.fromInstalledAgent());
            System.out.println("modify JavaCompiler");
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

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        return false;
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

    static class JavacCompilerGenerateAdvice {

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
    }

}
