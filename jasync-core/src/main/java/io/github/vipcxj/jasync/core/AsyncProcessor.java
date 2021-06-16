package io.github.vipcxj.jasync.core;

import com.google.auto.service.AutoService;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.*;
import io.github.vipcxj.jasync.core.javac.translator.PromiseTranslator;
import io.github.vipcxj.jasync.core.javac.translator.ReturnTranslator;
import io.github.vipcxj.jasync.core.javac.translator.SimplifiedTranslator;
import io.github.vipcxj.jasync.core.javac.translator.TryWithResourceTranslator;
import io.github.vipcxj.jasync.core.javac.visitor.PosVisitor;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.io.StringWriter;
import java.lang.reflect.Method;
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
            if (annotation.getQualifiedName().toString().equals("io.github.vipcxj.jasync.spec.annotations.Async")) {
                for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                    JavacTrees javacTrees = context.getTrees();
                    TreePath path = javacTrees.getPath(element);
                    CompilationUnitTree compilationUnit = path.getCompilationUnit();
                    IJAsyncCuContext cuContext = new JAsyncCuContext(context, compilationUnit);
                    JCTree.JCMethodDecl tree = (JCTree.JCMethodDecl) javacTrees.getTree(element);
                    StringWriter writer = new StringWriter();
                    tree.accept(new PosVisitor(writer, false));
                    System.out.println(writer.toString());
                    System.out.println();
                    tree.body.accept(new TryWithResourceTranslator(cuContext));
                    tree.body.accept(new ReturnTranslator(cuContext));
                    new PromiseTranslator(cuContext, false).reshape(tree);
                    tree.body.accept(new SimplifiedTranslator());
                    writer = new StringWriter();
                    tree.accept(new PosVisitor(writer, false));
                    System.out.println(writer.toString());
                    System.out.println(tree);
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
