package io.github.vipcxj.jasync.core.javac;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public class ReturnTranslator extends ShallowTranslator {

    private final IJAsyncCuContext context;
    private boolean findAwait;

    public ReturnTranslator(IJAsyncCuContext context) {
        this.context = context;
        this.findAwait = false;
    }

    @Override
    public void visitApply(JCTree.JCMethodInvocation jcMethodInvocation) {
        if (!findAwait) {
            Element element = context.getElement(jcMethodInvocation);
            if (element instanceof ExecutableElement) {
                if (element.getSimpleName().toString().equals("await")) {
                    Elements elementUtils = context.getEnvironment().getElementUtils();
                    Types typeUtils = context.getEnvironment().getTypeUtils();
                    TypeElement promiseElement = elementUtils.getTypeElement(Constants.PROMISE);
                    TypeMirror promiseType = promiseElement.asType();
                    if (typeUtils.isAssignable(element.getEnclosingElement().asType(), promiseType)) {
                        findAwait = true;
                    }
                }
            }
        }
        result = jcMethodInvocation;
    }

    @Override
    public void visitReturn(JCTree.JCReturn jcReturn) {
        if (findAwait) {
            TreeMaker treeMaker = context.getTreeMaker();
            Names names = context.getNames();
            int prePos = treeMaker.pos;
            result = treeMaker.at(jcReturn).Return(
                    treeMaker.Apply(
                            List.nil(),
                            JavacUtils.createQualifiedIdent(treeMaker, names, Constants.PROMISE_DO_RETURN),
                            List.of(jcReturn.expr)
                    )
            );
            treeMaker.pos = prePos;
        } else {
            result = jcReturn;
        }
    }

/*    @Override
    public void visitThrow(JCTree.JCThrow jcThrow) {
        TreeMaker treeMaker = context.getTreeMaker();
        Names names = context.getNames();
        int prePos = treeMaker.pos;
        treeMaker.pos = jcThrow.pos;
        result = treeMaker.at(jcThrow).Return(
                treeMaker.Apply(
                        List.nil(),
                        JavacUtils.createQualifiedIdent(treeMaker, names, Constants.PROMISE_ERROR),
                        List.of(jcThrow.expr)
                )
        );
        treeMaker.pos = prePos;
    }*/

/*    public static JCTree.JCStatement translateReturn(IJAsyncCuContext context, JCTree.JCStatement statement) {
        ReturnTranslator translator = new ReturnTranslator(context);
        statement.accept(translator);
        return (JCTree.JCStatement) translator.result;
    }

    public static List<JCTree.JCStatement> translateReturn(IJAsyncCuContext context, List<JCTree.JCStatement> statements) {
        return JavacUtils.mapList(statements, stat -> translateReturn(context, stat));
    }*/
}
