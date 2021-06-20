package io.github.vipcxj.jasync.core.javac.translator;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;

import javax.lang.model.SourceVersion;

public class TryWithResourceTranslator extends ShallowTranslator {

    private final IJAsyncInstanceContext context;

    public TryWithResourceTranslator(IJAsyncInstanceContext context) {
        this.context = context;
    }

    private boolean supportResource() {
        SourceVersion version = context.getEnvironment().getSourceVersion();
        return version.ordinal() >= SourceVersion.RELEASE_7.ordinal();
    }

    private JCTree.JCVariableDecl explainResource(JCTree tree) {
        if (tree instanceof JCTree.JCVariableDecl) {
            return (JCTree.JCVariableDecl) tree;
        } else if (tree instanceof JCTree.JCExpressionStatement) {
            JCTree.JCExpressionStatement statement = (JCTree.JCExpressionStatement) tree;
            return explainResource(statement.expr);
        } else {
            throw new IllegalArgumentException("Unable to get the resource declare.");
        }
    }

    private JCTree.JCBlock extractResource(List<JCTree> resources, JCTree.JCBlock body, JCDiagnostic.DiagnosticPosition pos) {
        if (resources == null || resources.isEmpty()) {
            return body;
        }
        TreeMaker treeMaker = context.getTreeMaker();
        int prePos = treeMaker.pos;
        Names names = context.getNames();
        JCTree.JCVariableDecl resource = explainResource(resources.head);
        Name var1 = names.fromString(context.nextVar());
        Name var2 = names.fromString(context.nextVar());
        Name nClose = names.fromString("close");
        JCTree.JCBlock block = treeMaker.at(pos).Block(0L, List.of(
                resource,
                treeMaker.Try(
                        extractResource(resources.tail, body, pos),
                        List.of(
                                treeMaker.Catch(
                                        treeMaker.VarDef(
                                                treeMaker.Modifiers(0L),
                                                var1,
                                                JavacUtils.makeQualifiedIdent(treeMaker, names, Throwable.class.getCanonicalName()),
                                                null
                                        ),
                                        treeMaker.Block(0L, List.of(
                                                treeMaker.Try(
                                                        treeMaker.Block(0L, List.of(
                                                                treeMaker.Exec(treeMaker.Apply(
                                                                        List.nil(),
                                                                        treeMaker.Select(treeMaker.Ident(resource.name), nClose),
                                                                        List.nil()
                                                                ))
                                                        )),
                                                        List.of(
                                                                treeMaker.Catch(
                                                                        treeMaker.VarDef(
                                                                                treeMaker.Modifiers(0L),
                                                                                var2,
                                                                                JavacUtils.makeQualifiedIdent(treeMaker, names, Throwable.class.getCanonicalName()),
                                                                                null
                                                                        ),
                                                                        treeMaker.Block(0L, List.of(
                                                                                treeMaker.Exec(treeMaker.Apply(
                                                                                        List.nil(),
                                                                                        treeMaker.Select(
                                                                                                treeMaker.Ident(var1),
                                                                                                names.fromString("addSuppressed")
                                                                                        ),
                                                                                        List.of(treeMaker.Ident(var2))
                                                                                ))
                                                                        ))
                                                                )
                                                        ),
                                                        null
                                                ),
                                                treeMaker.Throw(treeMaker.Ident(var1))
                                        ))
                                )
                        ),
                        null
                ),
                treeMaker.Exec(treeMaker.Apply(
                        List.nil(),
                        treeMaker.Select(treeMaker.Ident(resource.name), nClose),
                        List.nil()
                ))
        ));
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
