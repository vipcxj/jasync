package io.github.vipcxj.jasync.core;

import com.google.auto.service.AutoService;
import io.github.vipcxj.jasync.core.hack.Utils;
import io.github.vipcxj.jasync.core.javac.Constants;
import io.github.vipcxj.jasync.core.javac.IJAsyncContext;
import io.github.vipcxj.jasync.core.javac.JAsyncContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

@SupportedAnnotationTypes("io.github.vipcxj.jasync.spec.annotations.Async")
@AutoService(Processor.class)
public class AsyncProcessor extends AbstractProcessor {

    private IJAsyncContext context;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
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
        ProcessingEnvironment unwrappedProcessingEnv = JetbrainUtils.jbUnwrap(ProcessingEnvironment.class, this.processingEnv);
        this.context = new JAsyncContext(unwrappedProcessingEnv);
        System.out.println("Source version: " + unwrappedProcessingEnv.getSourceVersion());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (context.getLog().nerrors > 0) {
            return false;
        }
        for (TypeElement annotation : annotations) {
            if (annotation.getQualifiedName().toString().equals(Constants.ASYNC)) {
                for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                    try {
                        JavacUtils.processAsyncMethod(context, element);
                    } catch (RuntimeException t) {
                        t.printStackTrace();
                        throw t;
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
