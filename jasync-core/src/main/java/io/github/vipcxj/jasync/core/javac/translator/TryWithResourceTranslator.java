package io.github.vipcxj.jasync.core.javac.translator;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;
import io.github.vipcxj.jasync.core.javac.model.Ancestors;

import javax.lang.model.SourceVersion;

public class TryWithResourceTranslator extends ShallowTranslator {

    private final IJAsyncInstanceContext context;
    private final Ancestors ancestors;

    public TryWithResourceTranslator(IJAsyncInstanceContext context) {
        this.context = context;
        this.ancestors = new Ancestors();
    }

    private boolean supportResource() {
        SourceVersion version = context.getEnvironment().getSourceVersion();
        return version.ordinal() >= SourceVersion.RELEASE_7.ordinal();
    }

    private JCTree.JCBlock extractResource(List<JCTree> resources, JCTree.JCBlock body, JCDiagnostic.DiagnosticPosition pos) {
        if (resources == null || resources.isEmpty()) {
            return body;
        }
        TreeMaker treeMaker = context.getTreeMaker();
        int prePos = treeMaker.pos;
        Names names = context.getNames();
        Name var1 = names.fromString(context.nextVar());
        Name var2 = names.fromString(context.nextVar());
        Name nClose = names.fromString("close");
        Symbol owner = this.ancestors.enclosingMethod().sym;
        Symbol symRes = TreeInfo.symbol(resources.head);
        Symbol.VarSymbol symT1 = JavacUtils.createCatchThrowableParam(context, var1, owner);
        Symbol.VarSymbol symT2 = JavacUtils.createCatchThrowableParam(context, var2, owner);
        List<JCTree.JCStatement> statements = List.of(
                treeMaker.Try(
                        extractResource(resources.tail, body, pos),
                        List.of(
                                treeMaker.Catch(
                                        treeMaker.VarDef(symT1, null),
                                        JavacUtils.makeBlock(context, List.of(
                                                treeMaker.Try(
                                                        JavacUtils.makeBlock(context, List.of(
                                                                JavacUtils.makeExprStat(context, treeMaker.Apply(
                                                                        List.nil(),
                                                                        treeMaker.Select(treeMaker.Ident(symRes), nClose),
                                                                        List.nil()
                                                                ))
                                                        )),
                                                        List.of(
                                                                treeMaker.Catch(
                                                                        treeMaker.VarDef(symT2, null),
                                                                        JavacUtils.makeBlock(context, List.of(
                                                                                JavacUtils.makeExprStat(context, treeMaker.Apply(
                                                                                        List.nil(),
                                                                                        treeMaker.Select(
                                                                                                treeMaker.Ident(symT1),
                                                                                                names.fromString("addSuppressed")
                                                                                        ),
                                                                                        List.of(treeMaker.Ident(symT2))
                                                                                ))
                                                                        ))
                                                                )
                                                        ),
                                                        null
                                                ),
                                                treeMaker.Throw(treeMaker.Ident(symT1))
                                        ))
                                )
                        ),
                        null
                ),
                treeMaker.Exec(treeMaker.Apply(
                        List.nil(),
                        treeMaker.Select(treeMaker.Ident(symRes), nClose),
                        List.nil()
                ))
        );
        if (resources.head instanceof JCTree.JCStatement) {
            statements.prepend((JCTree.JCStatement) resources.head);
        }
        JCTree.JCBlock block = treeMaker.at(pos).Block(0L, statements);
        treeMaker.pos = prePos;
        return block;
    }

    @Override
    public void visitTry(JCTree.JCTry jcTry) {
        if (supportResource()) {
            List<JCTree> resources = jcTry.resources;
            if (resources == null || resources.isEmpty()) {
                result = jcTry;
            } else {
                List<JCTree.JCCatch> catchers = jcTry.catchers;
                JCTree.JCBlock finalizer = jcTry.finalizer;
                JCTree.JCBlock bodyWithResources = extractResource(resources, jcTry.body, jcTry);
                if ((catchers == null || catchers.isEmpty()) && finalizer == null) {
                    result = bodyWithResources;
                } else {
                    TreeMaker treeMaker = context.getTreeMaker();
                    int prePos = treeMaker.pos;
                    result = treeMaker.at(jcTry).Try(
                            bodyWithResources,
                            catchers,
                            finalizer
                    );
                    treeMaker.pos = prePos;
                }
            }
        } else {
            result = jcTry;
        }
    }
}
