package io.github.vipcxj.jasync.core;

import com.google.auto.service.AutoService;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.IJAsyncContext;
import io.github.vipcxj.jasync.core.javac.IJAsyncCuContext;
import io.github.vipcxj.jasync.core.javac.JAsyncContext;
import io.github.vipcxj.jasync.core.javac.JAsyncCuContext;

import javax.annotation.processing.*;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

@SupportedAnnotationTypes("io.github.vipcxj.jasync.core.CompareUseCase")
@AutoService(Processor.class)
public class CompareProcessor extends AbstractProcessor {

    private IJAsyncContext context;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        ProcessingEnvironment unwrappedProcessingEnv = JetbrainUtils.jbUnwrap(ProcessingEnvironment.class, this.processingEnv);
        this.context = new JAsyncContext(unwrappedProcessingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            if (annotation.getQualifiedName().toString().equals("io.github.vipcxj.jasync.core.CompareUseCase")) {
                for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                    JavacTrees javacTrees = context.getTrees();
                    TreePath path = javacTrees.getPath(element);
                    CompilationUnitTree compilationUnit = path.getCompilationUnit();
                    IJAsyncCuContext cuContext = new JAsyncCuContext(context, compilationUnit);
                    JCTree tree = cuContext.getTrees().getTree(element);
                    System.out.println(tree);
                }

            }
        }
        return true;
    }
}
