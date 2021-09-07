package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.translate.TransStatementContext;

import javax.lang.model.element.Name;

public abstract class AbstractTransStatementContext<T extends JCTree.JCStatement> extends AbstractTranslateContext<T> implements TransStatementContext<T> {

    private Name label;

    public AbstractTransStatementContext(AnalyzerContext analyzerContext, T tree) {
        super(analyzerContext, tree);
    }

    @Override
    public void setLabel(Name label) {
        this.label = label;
    }

    @Override
    public JCTree.JCLiteral makeLabelArg() {
        if (label != null) {
            String labelValue = label.toString();
            if (!labelValue.isEmpty()) {
                return safeMaker().Literal(labelValue);
            }
        }
        return safeMaker().Literal(TypeTag.BOT, null);
    }
}
