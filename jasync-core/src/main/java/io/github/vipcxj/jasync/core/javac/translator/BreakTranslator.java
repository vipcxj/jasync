package io.github.vipcxj.jasync.core.javac.translator;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import io.github.vipcxj.jasync.core.javac.Constants;
import io.github.vipcxj.jasync.core.javac.IJAsyncCuContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;

public class BreakTranslator extends ShallowTranslator {

    private final IJAsyncCuContext context;

    public BreakTranslator(IJAsyncCuContext context) {
        this.context = context;
    }

    @Override
    public void visitBreak(JCTree.JCBreak jcBreak) {
        TreeMaker maker = context.getTreeMaker();
        int prePos = maker.pos;
        result = maker.at(jcBreak).Exec(maker.Apply(
                List.nil(),
                JavacUtils.makeQualifiedIdent(context, Constants.JASYNC_DO_BREAK),
                List.nil()
        ));
        maker.pos = prePos;
    }

    public static List<JCTree.JCStatement> reshapeStatements(IJAsyncCuContext context, List<JCTree.JCStatement> statements) {
        BreakTranslator translator = new BreakTranslator(context);
        return JavacUtils.mapList(statements, statement -> {
            statement.accept(translator);
            return (JCTree.JCStatement) translator.result;
        });
    }
}
