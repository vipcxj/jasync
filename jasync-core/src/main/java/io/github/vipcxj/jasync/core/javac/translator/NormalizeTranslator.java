package io.github.vipcxj.jasync.core.javac.translator;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.*;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;
import io.github.vipcxj.jasync.core.javac.model.Ancestors;
import io.github.vipcxj.jasync.core.javac.visitor.SymbolReplacer;

import javax.lang.model.SourceVersion;

public class NormalizeTranslator extends TreeTranslator {

    private final IJAsyncInstanceContext context;
    private final Ancestors ancestors;
    private boolean containAwait;

    public NormalizeTranslator(IJAsyncInstanceContext context) {
        this.context = context;
        this.ancestors = new Ancestors();
        this.containAwait = false;
    }

    @Override
    public <T extends JCTree> T translate(T tree) {
        ancestors.enter(tree);
        try {
            return super.translate(tree);
        } finally {
            ancestors.exit();
        }
    }

    @Override
    public void visitApply(JCTree.JCMethodInvocation tree) {
        super.visitApply(tree);
        if (!containAwait && JavacUtils.isAwaitTree(context, tree)) {
            containAwait = true;
        }
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
        boolean preContainAwait = this.containAwait;
        this.containAwait = false;
        try {
            super.visitTry(jcTry);
            if (supportResource() && containAwait) {
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
                        try {
                            result = treeMaker.at(jcTry).Try(
                                    bodyWithResources,
                                    catchers,
                                    finalizer
                            );
                        } finally {
                            treeMaker.pos = prePos;
                        }
                    }
                }
            } else {
                result = jcTry;
            }
        } finally {
            containAwait = containAwait || preContainAwait;
        }
    }

    @Override
    public void visitSwitch(JCTree.JCSwitch tree) {
        if (tree.cases.size() == 1 && tree.cases.head.pat == null) {
            JCTree.JCCase jcCase = tree.cases.head;
            if (jcCase.stats == null) {
                result = JavacUtils.wrapPos(
                        context,
                        context.getTreeMaker().Skip(),
                        tree,
                        true
                );
            } else if (jcCase.stats.size() == 1) {
                result = JavacUtils.makeBlock(context, jcCase.stats.head);
            } else {
                result = JavacUtils.makeBlock(context, jcCase.stats);
            }
            result = translate(result);
            return;
        }
        tree.selector = translate(tree.selector);
        boolean preContainAwait = this.containAwait;
        this.containAwait = false;
        try {
            tree.cases = translate(tree.cases);
            if (containAwait) {
                List<JCTree.JCVariableDecl> decls = List.nil();
                for (JCTree.JCCase jcCase : tree.cases) {
                    if (jcCase.stats != null) {
                        JCTree.JCBlock block;
                        if (jcCase.stats.size() == 1) {
                            block = JavacUtils.makeBlock(context, jcCase.stats.head);
                        } else {
                            block = JavacUtils.makeBlock(context, jcCase.stats);
                        }
                        for (JCTree.JCVariableDecl decl : decls) {
                            TreeMaker maker = context.getTreeMaker();
                            int prePos = maker.pos;
                            maker.pos = block.pos;
                            try {
                                Symbol.VarSymbol sym = decl.sym;
                                Symbol.VarSymbol newSym = new Symbol.VarSymbol(sym.flags(), sym.name, sym.type, sym.owner);
                                newSym.pos = maker.pos;
                                JCTree.JCVariableDecl newDecl = maker.VarDef(newSym, null);
                                SymbolReplacer replacer = new SymbolReplacer(context, sym, newSym);
                                replacer.scan(block);
                                if (replacer.happened()) {
                                    block.stats = block.stats.prepend(newDecl);
                                }
                            } finally {
                                maker.pos = prePos;
                            }
                        }
                        for (JCTree.JCStatement stat : jcCase.stats) {
                            if (stat instanceof JCTree.JCVariableDecl) {
                                decls = decls.prepend((JCTree.JCVariableDecl) stat);
                            }
                        }
                        jcCase.stats = List.of(block);
                    }
                }
            }
            result = tree;
        } finally {
            containAwait = containAwait || preContainAwait;
        }
    }

    @Override
    public void visitForLoop(JCTree.JCForLoop tree) {
        boolean preContainAwait = this.containAwait;
        this.containAwait = false;
        try {
            super.visitForLoop(tree);
            result = tree;
            if (containAwait && tree.init != null && !tree.init.isEmpty()) {
                TreeMaker maker = context.getTreeMaker();
                int prePos = maker.pos;
                try {
                    ListBuffer<JCTree.JCStatement> newStatements = new ListBuffer<>();
                    ListBuffer<JCTree.JCStatement> newInits = new ListBuffer<>();
                    for (JCTree.JCStatement statement : tree.init) {
                        if (statement instanceof JCTree.JCVariableDecl) {
                            JCTree.JCVariableDecl decl = (JCTree.JCVariableDecl) statement;
                            if (decl.init != null) {
                                maker.at(statement);
                                JCTree.JCStatement newInit = maker.Exec(
                                        maker.Assign(
                                                maker.Ident(decl.sym),
                                                decl.init
                                        )
                                );
                                newInits = newInits.append(newInit);
                                decl.init = null;
                            }
                            newStatements = newStatements.append(decl);
                        } else {
                            newInits = newInits.append(statement);
                        }
                    }
                    tree.init = newInits.toList();
                    newStatements = newStatements.append(tree);
                    result = maker.Block(0L, newStatements.toList());
                } finally {
                    maker.pos = prePos;
                }
            }
        } finally {
            containAwait = containAwait || preContainAwait;
        }
    }

    @Override
    public void visitLabelled(JCTree.JCLabeledStatement tree) {
        boolean forLoop = tree.body instanceof JCTree.JCForLoop;
        JCTree.JCStatement body = tree.body;
        super.visitLabelled(tree);
        if (forLoop && tree.body instanceof JCTree.JCBlock) {
            TreeMaker maker = context.getTreeMaker();
            int prePos = maker.pos;
            try {
                JCTree.JCBlock block = (JCTree.JCBlock) tree.body;
                ListBuffer<JCTree.JCStatement> newStatements = new ListBuffer<>();
                int i = 0;
                for (JCTree.JCStatement statement : block.stats) {
                    if (++i == block.stats.size()) {
                        newStatements = newStatements.append(
                                maker.at(tree).Labelled(tree.label, body)
                        );
                    } else {
                        newStatements = newStatements.append(statement);
                    }
                }
                block.stats = newStatements.toList();
                result = block;
            } finally {
                maker.pos = prePos;
            }
        }
    }

    @Override
    public void visitAssignop(JCTree.JCAssignOp tree) {
        boolean preContainAwait = this.containAwait;
        this.containAwait = false;
        try {
            super.visitAssignop(tree);
            if (this.containAwait) {
                TreeMaker maker = context.getTreeMaker();
                int prePos = maker.pos;
                try {
                    JCTree.Tag opCode = tree.getTag().noAssignOp();
                    JCTree.JCExpression lhs = new TreeCopier<>(maker).copy(tree.lhs);
                    TreeInfo.setSymbol(lhs, TreeInfo.symbol(tree.lhs));
                    lhs.type = tree.lhs.type;
                    JCTree.JCExpression newLhs = maker.at(lhs).Binary(opCode, lhs, tree.rhs);
                    result = maker.at(tree).Assign(tree.lhs, newLhs);
                } finally {
                    maker.pos = prePos;
                }
            }
        } finally {
            containAwait = containAwait || preContainAwait;
        }
    }
}
