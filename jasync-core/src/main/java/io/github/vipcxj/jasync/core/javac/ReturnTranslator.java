package io.github.vipcxj.jasync.core.javac;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;

public class ReturnTranslator extends ShallowTranslator {

    private final IJAsyncContext context;

    public ReturnTranslator(IJAsyncContext context) {
        this.context = context;
    }

    @Override
    public void visitReturn(JCTree.JCReturn jcReturn) {
        TreeMaker treeMaker = context.getTreeMaker();
        Names names = context.getNames();
        int prePos = treeMaker.pos;
        treeMaker.pos = jcReturn.pos;
        result = treeMaker.Return(
                treeMaker.Apply(
                        null,
                        JavacUtils.createQualifiedIdent(treeMaker, names, "io.github.vipcxj.jasync.spec.Promise.doReturn"),
                        List.of(jcReturn.expr)
                )
        );
        treeMaker.pos = prePos;
    }

    public static JCTree.JCBlock translateReturn(IJAsyncContext context, JCTree.JCBlock block) {
        block.accept(new ReturnTranslator(context));
        return block;
    }

    public static JCTree.JCStatement translateReturn(IJAsyncContext context, JCTree.JCStatement statement) {
        ReturnTranslator translator = new ReturnTranslator(context);
        statement.accept(translator);
        return (JCTree.JCStatement) translator.result;
    }

    public static List<JCTree.JCStatement> translateReturn(IJAsyncContext context, List<JCTree.JCStatement> statements) {
        return JavacUtils.mapList(statements, stat -> translateReturn(context, stat));
    }
}
