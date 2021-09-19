package io.github.vipcxj.jasync.core;

import com.google.auto.service.AutoService;
import io.github.vipcxj.jasync.utils.hack.Utils;
import io.github.vipcxj.jasync.core.javac.Constants;
import io.github.vipcxj.jasync.core.javac.IJAsyncContext;
import io.github.vipcxj.jasync.core.javac.JAsyncContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;
import io.github.vipcxj.jasync.core.log.LogManager;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@SupportedAnnotationTypes("io.github.vipcxj.jasync.spec.annotations.Async")
@AutoService(Processor.class)
public class AsyncProcessor extends AbstractProcessor {

    private static final Logger logger = LogManager.INSTANCE.createLogger(AsyncProcessor.class);

    private IJAsyncContext context;
    private boolean valid = false;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        try {
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
            logger.log(Level.SEVERE, t, t::getLocalizedMessage);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!valid) {
            return false;
        }
        if (context.getLog().nerrors > 0) {
            return false;
        }
        for (TypeElement annotation : annotations) {
            if (annotation.getQualifiedName().toString().equals(Constants.ASYNC)) {
                for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                    try {
                        JavacUtils.processAsyncMethod(context, element);
                    } catch (Throwable t) {
                        logger.throwing(this.getClass().getCanonicalName(), "process", t);
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }
}
