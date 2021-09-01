package io.github.vipcxj.jasync.core;

import com.google.auto.service.AutoService;
import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.*;
import io.github.vipcxj.jasync.core.javac.translate.context.TransMethodContext;
import io.github.vipcxj.jasync.core.javac.translator.*;
import io.github.vipcxj.jasync.core.javac.visitor.JAsyncAnalyzer;
import io.github.vipcxj.jasync.core.javac.visitor.PosVisitor;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.io.StringWriter;
import java.util.Set;

@SupportedAnnotationTypes("io.github.vipcxj.jasync.spec.annotations.Async")
@AutoService(Processor.class)
public class AsyncProcessor extends AbstractProcessor {

    private IJAsyncContext context;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        System.out.println("AsyncProcessor init.");
        ProcessingEnvironment unwrappedProcessingEnv = JetbrainUtils.jbUnwrap(ProcessingEnvironment.class, this.processingEnv);
        this.context = new JAsyncContext(unwrappedProcessingEnv);
        System.out.println("Source version: " + unwrappedProcessingEnv.getSourceVersion());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            if (annotation.getQualifiedName().toString().equals(Constants.ASYNC)) {
                for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                    IJAsyncInstanceContext instanceContext = new JAsyncInstanceContext(context, (ExecutableElement) element);
                    if (instanceContext.getInfo().isEnabled()) {
                        JCTree.JCMethodDecl tree = (JCTree.JCMethodDecl) instanceContext.getTrees().getTree(element);
                        StringWriter writer = new StringWriter();
                        // tree.accept(new PosVisitor(writer, false));
                        // System.out.println(writer.toString());
                        // System.out.println();
                        new NormalizeTranslator(instanceContext).translate(tree);
                        TransMethodContext transContext = JAsyncAnalyzer.scan(instanceContext, tree);
                        transContext.complete();
                        transContext.lock();
                        transContext.buildTree(true);
//                        tree.body.accept(new TryWithResourceTranslator(instanceContext));
//                        tree.body.accept(new ReturnTranslator(instanceContext));
//                        new PromiseTranslator(instanceContext, false).reshape(tree);
//                        tree.body.accept(new SimplifiedTranslator());
                        writer = new StringWriter();
                        if (instanceContext.getInfo().isLogResultPosTree()) {
                            tree.accept(new PosVisitor(writer, false));
                            System.out.println(writer.toString());
                        }
                        if (instanceContext.getInfo().isLogResultTree()) {
                            System.out.println(tree);
                        }
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
