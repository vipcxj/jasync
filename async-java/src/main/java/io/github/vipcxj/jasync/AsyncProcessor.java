package io.github.vipcxj.jasync;

import com.google.auto.service.AutoService;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("io.github.vipcxj.jasync.annotations.Async")
@AutoService(Processor.class)
public class AsyncProcessor extends AbstractProcessor {

    private JavacTrees javacTrees; // 提供了待处理的抽象语法树
    private TreeMaker treeMaker; // 封装了创建AST节点的一些方法
    private Names names; // 提供了创建标识符的方法

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        System.out.println("AsyncProcessor init.");
        this.javacTrees = JavacTrees.instance(processingEnv);
        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        this.treeMaker = TreeMaker.instance(context);
        this.names = Names.instance(context);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            if (annotation.getQualifiedName().toString().equals("io.github.vipcxj.jasync.annotations.Async")) {
                for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                    JCTree.JCMethodDecl tree = (JCTree.JCMethodDecl) javacTrees.getTree(element);
                    for (JCTree.JCStatement statement : tree.getBody().getStatements()) {
                        System.out.println(statement);
                        System.out.println(statement.getClass());
                    }

                    tree.accept(new TreeTranslator() {
                        @Override
                        public void visitIdent(JCTree.JCIdent tree) {
                            System.out.println("visitIdent");
                            System.out.println(tree);
                            super.visitIdent(tree);
                        }

                        @Override
                        public void visitSelect(JCTree.JCFieldAccess tree) {
                            System.out.println("visitSelect");
                            System.out.println(tree);
                            super.visitSelect(tree);
                        }

                        @Override
                        public void visitApply(JCTree.JCMethodInvocation tree) {
                            System.out.println("visitApply");
                            JCTree.JCExpression methodSelect = tree.getMethodSelect();
                            System.out.println(methodSelect);
                            System.out.println(methodSelect.getClass());
                            if (methodSelect instanceof JCTree.JCIdent) {
                                System.out.println(((JCTree.JCIdent) methodSelect).sym);
                            }
                            super.visitApply(tree);
                        }
                    });
                }
            }
        }
        return true;
    }
}
