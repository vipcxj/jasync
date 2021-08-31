package io.github.vipcxj.jasync.core;

import com.google.auto.service.AutoService;
import com.sun.source.tree.LineMap;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.List;
import io.github.vipcxj.jasync.core.javac.IJAsyncContext;
import io.github.vipcxj.jasync.core.javac.JAsyncContext;
import io.github.vipcxj.jasync.core.javac.visitor.ExtractMethodScanner;

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
                    JCTree tree = context.getTrees().getTree(element);
                    TreePath path = context.getTrees().getPath(element);
                    LineMap lineMap = path.getCompilationUnit().getLineMap();
                    tree.accept(new TreeScanner() {
                        @Override
                        public void visitBlock(JCTree.JCBlock tree) {
                            List<JCTree.JCStatement> testStatements = List.nil();
                            boolean testing = false;
                            for (JCTree.JCStatement statement : tree.stats) {
                                if (testing) {
                                    if (statement instanceof JCTree.JCLabeledStatement) {
                                        JCTree.JCLabeledStatement labeledStatement = (JCTree.JCLabeledStatement) statement;
                                        if (labeledStatement.label.toString().equals("testEnd")) {
                                            testing = false;
                                            continue;
                                        }
                                    }
                                    testStatements = testStatements.append(statement);
                                } else if (statement instanceof JCTree.JCLabeledStatement) {
                                    JCTree.JCLabeledStatement labeledStatement = (JCTree.JCLabeledStatement) statement;
                                    if (labeledStatement.label.toString().equals("testStart")) {
                                        testing = true;
                                        testStatements = testStatements.append(labeledStatement.body);
                                    }
                                }
                            }
                            if (!testing) {
                                ExtractMethodScanner.ExtractMethodContext context = ExtractMethodScanner.scanStatements(lineMap, testStatements);
                                context.printCaptured();
                            }
                            super.visitBlock(tree);
                        }
                    });
                }

            }
        }
        return true;
    }
}
