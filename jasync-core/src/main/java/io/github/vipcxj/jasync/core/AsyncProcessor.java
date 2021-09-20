package io.github.vipcxj.jasync.core;

import com.google.auto.service.AutoService;
import io.github.vipcxj.jasync.core.javac.Constants;
import io.github.vipcxj.jasync.core.javac.IJAsyncContext;
import io.github.vipcxj.jasync.core.javac.JAsyncContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;
import io.github.vipcxj.jasync.utils.hack.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.Set;

@SupportedAnnotationTypes("io.github.vipcxj.jasync.spec.annotations.Async")
@AutoService(Processor.class)
public class AsyncProcessor extends AbstractProcessor {

    private static final Logger logger = LogManager.getLogger();

    private IJAsyncContext context;
    private boolean valid = false;

/*    private void createFile(String name) {
        try {
            String fileName = name + ".check";
            File file = new File(fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }*/

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
/*        if (logger.isTraceEnabled()) {
            createFile("log-enabled");
        } else {
            createFile("log-not-enabled");
        }
        InputStream io = getClass().getResourceAsStream("/log4j2.xml");
        if (io != null) {
            createFile("log4j2-exits");
        } else {
            createFile("log4j2-not-exits");
        }*/
        try {
            logger.traceEntry("init");
            System.out.println("AsyncProcessor init.");
            Utils.addOpensForLombok(AsyncProcessor.class, new String[] {
                    "com.sun.tools.javac.api",
                    "com.sun.tools.javac.code",
                    "com.sun.tools.javac.comp",
                    "com.sun.tools.javac.model",
                    "com.sun.tools.javac.processing",
                    "com.sun.tools.javac.tree",
                    "com.sun.tools.javac.util"
            });
            logger.info("The processingEnv real type: " + processingEnv.getClass().getName() + ".");
            ProcessingEnvironment unwrappedProcessingEnv = Utils.getJavacProcessingEnvironment(this.processingEnv, this.processingEnv);
            this.context = new JAsyncContext(unwrappedProcessingEnv);
            this.valid = true;
        } catch (Throwable t) {
            logger.catching(t);
        } finally {
            logger.traceExit();
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        logger.traceEntry("process");
        if (!valid) {
            return logger.traceExit(false);
        }
        if (context.getLog().nerrors > 0) {
            return logger.traceExit(false);
        }
        for (TypeElement annotation : annotations) {
            if (annotation.getQualifiedName().toString().equals(Constants.ASYNC)) {
                for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                    if (element instanceof ExecutableElement) {
                        try {
                            logger.info(() -> JavacUtils.printMethod((ExecutableElement) element));
                            JavacUtils.processAsyncMethod(context, element);
                        } catch (Throwable t) {
                            logger.catching(t);
                            return logger.traceExit(false);
                        }
                    }
                }
            }
        }
        return logger.traceExit(true);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        logger.traceEntry("getSupportedSourceVersion");
        return logger.traceExit(SourceVersion.latest());
    }
}
