package io.github.vipcxj.jasync.core.javac.translator;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;
import io.github.vipcxj.jasync.core.javac.Constants;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;
import io.github.vipcxj.jasync.core.javac.model.VarInfo;
import io.github.vipcxj.jasync.core.javac.model.VarKey;
import io.github.vipcxj.jasync.core.javac.visitor.AwaitScanner;
import io.github.vipcxj.jasync.core.javac.visitor.PosSetterScanner;
import io.github.vipcxj.jasync.core.javac.visitor.ScopeVarScanner;
import io.github.vipcxj.jasync.core.javac.visitor.SwitchScanner;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Iterator;
import java.util.Map;

public class PromiseTranslator extends TreeTranslator {

    private final IJAsyncInstanceContext context;
    private ACTION action;
    private JCTree.JCExpression unwrap;
    private final String replacedVar;
    private Type awaitType;
    private boolean complete;
    private boolean promise;
    private JCTree.JCStatement toReshape;
    private final boolean reshaped;

    public PromiseTranslator(IJAsyncInstanceContext jAsyncContext, boolean reshaped) {
        this.context = jAsyncContext;
        this.action = ACTION.NO_OP;
        this.unwrap = null;
        this.replacedVar = context.nextVar();
        this.complete = false;
        this.promise = false;
        this.reshaped = reshaped;
    }

    public PromiseTranslator copy() {
        return new PromiseTranslator(context, reshaped);
    }

    public PromiseTranslator copyReshaped() {
        return new PromiseTranslator(context, true);
    }

    public ProcessingEnvironment getEnvironment() {
        return context.getEnvironment();
    }

    public Context getContext() {
        return context.getContext();
    }

    public TreeMaker getTreeMaker() {
        return context.getTreeMaker();
    }

    public Names getNames() {
        return context.getNames();
    }

    public JavacTrees getTrees() {
        return context.getTrees();
    }


    public Element getElement(JCTree tree) {
        return context.getElement(tree);
    }

    @Override
    public <T extends JCTree> T translate(T t) {
        if (complete) {
            return t;
        }
        return super.translate(t);
    }

    @Override
    public void visitTopLevel(JCTree.JCCompilationUnit jcCompilationUnit) {
        super.visitTopLevel(jcCompilationUnit);
    }

    private JCTree.JCExpression unwrap(JCTree.JCMethodInvocation jcMethodInvocation) {
        JCTree.JCExpression methodSelect = jcMethodInvocation.getMethodSelect();
        if (methodSelect instanceof  JCTree.JCFieldAccess) {
            return ((JCTree.JCFieldAccess) methodSelect).getExpression();
        } else  {
            throw new IllegalArgumentException("This is impossible!");
        }
    }

    @Override
    public void visitApply(JCTree.JCMethodInvocation jcMethodInvocation) {
        if (!complete) {
            Element element = getElement(jcMethodInvocation);
            if (element instanceof ExecutableElement) {
                if (element.getSimpleName().toString().equals("await")) {
                    Elements elementUtils = getEnvironment().getElementUtils();
                    Types typeUtils = getEnvironment().getTypeUtils();
                    TypeElement promiseElement = elementUtils.getTypeElement(Constants.PROMISE);
                    TypeMirror promiseType = promiseElement.asType();
                    if (typeUtils.isAssignable(element.getEnclosingElement().asType(), promiseType)) {
                        System.out.println("find await at " + jcMethodInvocation.getPreferredPosition());
                        this.action = ACTION.RESHAPE_AWAIT;
                        this.unwrap = unwrap(jcMethodInvocation);
                        this.awaitType = jcMethodInvocation.type;
                        this.result = JavacUtils.wrapPos(
                                context,
                                getTreeMaker().Ident(getNames().fromString(replacedVar)),
                                jcMethodInvocation,
                                true
                        );
                        this.complete = true;
                        return;
                    }
                }
            }
        }
        super.visitApply(jcMethodInvocation);
    }

    @Override
    public void visitBlock(JCTree.JCBlock jcBlock) {
        List<JCTree.JCStatement> oldStats = jcBlock.stats;
        List<JCTree.JCStatement> stats = copy().reshapeStatements(jcBlock.stats, blockStatementsReplacer(jcBlock));
        if (oldStats != stats) {
            jcBlock.stats = stats;
            action = ACTION.RESHAPE_STATEMENT;
            this.complete = true;
        }
        result = jcBlock;
    }

    @Override
    public void visitIf(JCTree.JCIf jcIf) {
        jcIf.cond = translate(jcIf.cond);
        if (!complete) {
            JCTree.JCStatement thenPart = copy().reshapeStatement(jcIf.thenpart, ifThenStatementsReplacer(jcIf));
            JCTree.JCStatement elsePart = copy().reshapeStatement(jcIf.elsepart, toIfElseStatementsReplacer(jcIf));
            if (thenPart != jcIf.thenpart || elsePart != jcIf.elsepart) {
                action = ACTION.RESHAPE_STATEMENT;
                this.complete = true;
            }
            jcIf.thenpart = thenPart;
            jcIf.elsepart = elsePart;
            result = jcIf;
        } else {
            super.visitIf(jcIf);
        }
    }

    @Override
    public void visitSwitch(JCTree.JCSwitch jcSwitch) {
        jcSwitch.selector = translate(jcSwitch.selector);
        if (!complete) {
            if (jcSwitch.cases == null || jcSwitch.cases.isEmpty()) {
                result = jcSwitch;
                return;
            }
            // only default block
            if (jcSwitch.cases.size() == 1 && jcSwitch.cases.head.pat == null) {
                List<JCTree.JCStatement> statements = jcSwitch.cases.head.stats;
                jcSwitch.cases.head.stats = copyReshaped().reshapeStatements(statements, caseStatementsReplacer(jcSwitch.cases.head));
                if (statements != jcSwitch.cases.head.stats) {
                    complete = true;
                    action = ACTION.RESHAPE_STATEMENT;
                    result = JavacUtils.makeBlock(context, jcSwitch.cases.head.stats);
                } else {
                    result = jcSwitch;
                }
                return;
            }
            if (!AwaitScanner.checkTree(context, jcSwitch)) {
                result = jcSwitch;
                return;
            }
            SwitchScanner scanner = new SwitchScanner(context);
            jcSwitch.accept(scanner);
            TreeMaker treeMaker = context.getTreeMaker();
            int prePos = treeMaker.pos;
            try {
                Map<JCTree.JCCase, java.util.List<Symbol.VarSymbol>> redeclareVarsMap = scanner.collectNeedRedeclareVars();
                ListBuffer<JCTree.JCBlock> cases = new ListBuffer<>();
                for (JCTree.JCCase jcCase : jcSwitch.cases) {
                    List<JCTree.JCStatement> statements = jcCase.stats;
                    JavacUtils.atCaseBlockStart(context, jcCase);
                    if (statements != null && !statements.isEmpty()) {
                        java.util.List<Symbol.VarSymbol> redeclareVars = redeclareVarsMap.get(jcCase);
                        if (redeclareVars != null) {
                            for (Symbol.VarSymbol redeclareVar : redeclareVars) {
                                JCTree.JCVariableDecl varDef = treeMaker.VarDef(redeclareVar, null);
                                varDef.sym = null;
                                statements = statements.prepend(varDef);
                                jcCase.accept(new CaseVarCleanSymTranslator(context, redeclareVar));
                            }
                        }
                        statements = BreakTranslator.reshapeStatements(context, statements);
                        JCTree.JCBlock block = JavacUtils.makeBlock(context, statements);
                        // make new declare valid in the ast tree.
                        jcCase.stats = List.of(block);
                        JavacUtils.attrStat(context, block);
                        statements = copyReshaped().reshapeStatements(statements, blockStatementsReplacer(block));
                        block.stats = statements;
                        // restore the case statements.
                        cases.append(block);
                    } else {
                        cases.append(JavacUtils.makeBlock(context, List.nil()));
                    }
                }
                complete = true;
                action = ACTION.RESHAPE_STATEMENT;
                promise = true;
                ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
                Iterator<JCTree.JCBlock> iterator = cases.iterator();
                for (JCTree.JCCase aCase : jcSwitch.cases) {
                    JCTree.JCBlock block = iterator.next();
                    treeMaker.at(block);
                    int caseType = JavacUtils.getCaseType(context, aCase);
                    args.append(JavacUtils.makeCaseWithBlock(context, caseType, aCase.pat, block));
                }
                treeMaker.at(jcSwitch);
                result = JavacUtils.makeExprStat(context, JavacUtils.makeApply(
                        context,
                        Constants.JASYNC_DO_SWITCH,
                        List.of(
                                jcSwitch.selector,
                                JavacUtils.makeApply(
                                        context,
                                        Constants.CASES_OF,
                                        args.toList()
                                )
                        )
                ));
            } finally {
                treeMaker.pos = prePos;
            }
        } else {
            result = jcSwitch;
        }
    }

    @Override
    public void visitForeachLoop(JCTree.JCEnhancedForLoop jcEnhancedForLoop) {
        jcEnhancedForLoop.expr = translate(jcEnhancedForLoop.expr);
        if (!complete) {
            JCTree.JCStatement oldBody = jcEnhancedForLoop.body;
            JCTree.JCStatement newBody = copy().reshapeStatement(oldBody, toForeachBodyStatementsReplacer(jcEnhancedForLoop));
            if (oldBody != newBody) {
                complete = true;
                promise = true;
                action = ACTION.RESHAPE_STATEMENT;
                TreeMaker maker = context.getTreeMaker();
                int prePos = maker.pos;
                try {
                    maker.at(jcEnhancedForLoop);
                    result = maker.Exec(JavacUtils.makeForEach(context, jcEnhancedForLoop));
                } finally {
                    maker.pos = prePos;
                }
            } else {
                this.result = jcEnhancedForLoop;
            }
        } else {
            this.result = jcEnhancedForLoop;
        }
    }

    private void visitWhileLikeLoop(JCTree.JCExpression cond, JCTree.JCStatement body, StatementsReplacer replacer, String method) {
        if (!complete) {
            JCTree.JCStatement newBody = copyReshaped().reshapeStatement(body, replacer);
            PromiseTranslator translator = copyReshaped();
            cond.accept(translator);
            if (translator.complete || newBody != body) {
                JCTree.JCExpression argCond;
                if (translator.complete) {
                    if (translator.action != ACTION.RESHAPE_AWAIT) {
                        throw new IllegalStateException("Invalid action here: " + translator.action + ".");
                    }
                    JCTree.JCBlock block = JavacUtils.makeBlock(
                            context,
                            JavacUtils.makeReturn(
                                    context,
                                    JavacUtils.makePromise(
                                            context,
                                            (JCTree.JCExpression) translator.result
                                    )
                            )
                    );
                    translator.unwrap.accept(new PosSetterScanner(block.getStartPosition()));
                    JCTree.JCExpression condExpr = JavacUtils.makeApply(
                            context,
                            Constants.THEN,
                            List.of(JavacUtils.makePromiseFunction(
                                    context,
                                    block,
                                    translator.awaitType,
                                    context.getTypes().boxedTypeOrType(context.getSymbols().booleanType),
                                    translator.replacedVar
                            )),
                            translator.unwrap
                    );

                    argCond =  JavacUtils.makeBooleanPromiseSupplier(context, condExpr);
                } else {
                    argCond = JavacUtils.makeBooleanSupplier(context, cond);
                }
                complete = true;
                promise = true;
                action = ACTION.RESHAPE_STATEMENT;
                TreeMaker maker = getTreeMaker();
                int prePos = maker.pos;
                try {
                    result = maker.Exec(JavacUtils.makeApply(
                            context,
                            method,
                            List.of(
                                    argCond,
                                    JavacUtils.makeVoidPromiseSupplier(
                                            context,
                                            JavacUtils.makeBlock(context, newBody)
                                    )
                            )
                    ));
                } finally {
                    maker.pos = prePos;
                }
            }
        }
    }

    @Override
    public void visitWhileLoop(JCTree.JCWhileLoop jcWhileLoop) {
        if (!complete) {
            visitWhileLikeLoop(jcWhileLoop.cond,
                    jcWhileLoop.body,
                    toWhileBodyStatementsReplacer(jcWhileLoop),
                    Constants.JASYNC_DO_WHILE
            );
            if (!complete) {
                this.result = jcWhileLoop;
            }
        } else {
            this.result = jcWhileLoop;
        }
    }

    @Override
    public void visitDoLoop(JCTree.JCDoWhileLoop jcDoWhileLoop) {
        if (!complete) {
            visitWhileLikeLoop(
                    jcDoWhileLoop.cond,
                    jcDoWhileLoop.body,
                    toDoWhileBodyStatementsReplacer(jcDoWhileLoop),
                    Constants.JASYNC_DO_DO_WHILE
            );
            if (!complete) {
                this.result = jcDoWhileLoop;
            }
        } else {
            this.result = jcDoWhileLoop;
        }
    }

    @Override
    public void visitForLoop(JCTree.JCForLoop jcForLoop) {
        AwaitScanner scanner = new AwaitScanner(context);
        if (Boolean.TRUE.equals(jcForLoop.accept(scanner, null))) {
            this.result = translate(JavacUtils.makeWhileFromForLoop(context, jcForLoop));
        } else {
            this.result = jcForLoop;
        }
    }

    @Override
    public void visitTry(JCTree.JCTry jcTry) {
        if (jcTry == toReshape) {
            jcTry.resources = translate(jcTry.resources);
            if (!complete) {
                if (copyReshaped().reshapeBlock(jcTry.body) || copyReshaped().visitCatches(jcTry.catchers) || copyReshaped().reshapeBlock(jcTry.finalizer)) {
                    this.action = ACTION.RESHAPE_TRY;
                    this.complete = true;
                    this.promise = true;
                    TreeMaker maker = context.getTreeMaker();
                    int prePos = maker.pos;
                    try {
                        this.result = maker.at(jcTry).Exec(JavacUtils.createPromiseThen(
                                context,
                                jcTry.body,
                                jcTry.catchers,
                                jcTry.finalizer,
                                null
                        ));
                    } finally {
                        maker.pos = prePos;
                    }
                    return;
                }
            }
            result = jcTry;
        } else {
            throw new UnsupportedOperationException();
            // result = copy().reshapeStatement(jcTry);
        }
    }

    @Override
    public void visitLambda(JCTree.JCLambda jcLambda) {
        this.result = jcLambda;
    }

    @Override
    public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
        this.result = jcClassDecl;
    }

    public void reshape(JCTree.JCMethodDecl methodDecl) {
        reshapeBlock(methodDecl.body);
    }

    public boolean visitCatches(List<JCTree.JCCatch> catches) {
        if (catches == null) return false;
        boolean changed = false;
        for (JCTree.JCCatch jcCatch : catches) {
            changed = changed || reshapeBlock(jcCatch.body);
        }
        return changed;
    }

    public boolean reshapeBlock(JCTree.JCBlock block) {
        if (block == null) return false;
        List<JCTree.JCStatement> oldStats = block.stats;
        List<JCTree.JCStatement> stats = reshapeStatements(block.stats, blockStatementsReplacer(block));
        if (stats != oldStats) {
            block.stats = stats;
            return true;
        } else {
            return false;
        }
    }

    public JCTree.JCStatement reshapeStatement(JCTree.JCStatement statement, StatementsReplacer replacer) {
        if (statement == null) return null;
        if (statement instanceof JCTree.JCBlock) {
            JCTree.JCBlock jcBlock = (JCTree.JCBlock) statement;
            if (reshapeBlock(jcBlock)) {
                List<JCTree.JCStatement> stats = jcBlock.stats;
                JCTree.JCStatement out;
                if (stats == null || stats.isEmpty()) {
                    out = null;
                } else if (stats.size() == 1) {
                    out = stats.head;
                } else {
                    out = JavacUtils.shallowCopyBlock(context, jcBlock);
                }
                replacer.replace(out != null ? List.of(out) : null, true);
                return out;
            } else {
                return statement;
            }
        } else {
            List<JCTree.JCStatement> reshapedStatements = reshapeStatements(List.of(statement), replacer);
            assert reshapedStatements.size() == 1;
            return reshapedStatements.head;
        }
    }

    public List<JCTree.JCStatement> reshapeStatements(List<JCTree.JCStatement> statements, StatementsReplacer replacer) {
        TreeMaker treeMaker = getTreeMaker();
        Iterator<JCTree.JCStatement> iterator = statements.iterator();
        List<JCTree.JCStatement> heads = List.nil();
        List<JCTree.JCStatement> tails = List.nil();
        List<JCTree.JCStatement> newStatements;
        JCTree.JCExpression reshapedExpr;
        boolean append = false;
        JCTree.JCStatement current = null;
        while (iterator.hasNext()){
            JCTree.JCStatement statement = iterator.next();
            if (!append) {
                this.toReshape = statement;
                statement.accept(this);
                if (this.action != ACTION.NO_OP) {
                    current = (JCTree.JCStatement) result;
                    append = true;
                } else {
                    heads = heads.append(statement);
                }
            } else {
                tails = tails.append(statement);
            }
        }
        if (current == null) {
            return statements;
        }
        if (action == ACTION.RESHAPE_AWAIT) {
            if (current instanceof JCTree.JCExpressionStatement) {
                if (((JCTree.JCExpressionStatement) current).expr instanceof JCTree.JCIdent) {
                    current = null;
                }
            }
            if (current != null) {
                tails = tails.prepend(current);
            }
            int prePos = treeMaker.pos;
            if (tails.isEmpty()) {
                reshapedExpr = JavacUtils.makeApply(
                        context,
                        Constants.THEN_VOID,
                        List.nil(),
                        unwrap
                );
            } else {
                tails = copyReshaped().reshapeStatements(tails, prependStatementsReplacer(replacer, heads));
                JCTree.JCExpression voidPromiseFunction = JavacUtils.makeVoidPromiseFunction(
                        context,
                        JavacUtils.makeBlock(context, tails),
                        awaitType,
                        replacedVar
                );
                unwrap.accept(new PosSetterScanner(voidPromiseFunction.getStartPosition()));
                reshapedExpr = JavacUtils.makeApply(
                        context,
                        Constants.THEN_VOID,
                        List.of(voidPromiseFunction),
                        unwrap
                );
            }
            treeMaker.pos = prePos;
        } else if (action == ACTION.RESHAPE_STATEMENT) {
            int prePos = treeMaker.pos;
            if (!promise) {
                reshapedExpr = JavacUtils.makeApply(
                        context,
                        Constants.JASYNC_DEFER_VOID,
                        List.of(
                                JavacUtils.makeVoidPromiseSupplier(
                                        context,
                                        current.getKind() == Tree.Kind.BLOCK ? (JCTree.JCBlock) current : JavacUtils.makeBlock(context, current)
                                )
                        )
                );
            } else {
                if (current instanceof JCTree.JCExpressionStatement) {
                    reshapedExpr = ((JCTree.JCExpressionStatement) current).expr;
                } else {
                    throw new IllegalStateException("This is impossible.");
                }
            }
            if (!tails.isEmpty()) {
                reshapedExpr = JavacUtils.makeApply(
                        context,
                        Constants.THEN_VOID,
                        List.of(
                                JavacUtils.makeVoidPromiseSupplier(
                                        context,
                                        JavacUtils.makeBlock(
                                                context,
                                                copyReshaped().reshapeStatements(tails, prependStatementsReplacer(replacer, heads))
                                        )
                                )
                        ),
                        reshapedExpr
                );
            }
            treeMaker.pos = prePos;
        } else {
            return statements;
        }
        if (reshapedExpr != null) {
            Map<VarKey, VarInfo> varData = ScopeVarScanner.scanVar(context, heads, reshapedExpr);
            ListBuffer<JCTree.JCStatement> decls1 = JavacUtils.createVarDecls(context, varData, reshapedExpr.getStartPosition());
            heads = heads.appendList(decls1);
            reshapedExpr.accept(new ScopeVarTranslator(context, varData));
            if (!decls1.isEmpty()) {
                ListBuffer<JCTree.JCStatement> decls2 = JavacUtils.resumeVarDecls(context, varData, reshapedExpr.getStartPosition());
                reshapedExpr = JavacUtils.makeApply(
                        context,
                        Constants.JASYNC_DEFER_VOID,
                        List.of(JavacUtils.makeVoidPromiseSupplier(
                                context,
                                JavacUtils.makeBlock(
                                        context,
                                        decls2.toList().append(
                                                JavacUtils.makeReturn(context, reshapedExpr)
                                        )
                                )
                        ))
                );
            }
            if (!reshaped) {
                reshapedExpr = JavacUtils.appendCatchReturn(context, reshapedExpr);
            }
        }
        newStatements = heads.append(JavacUtils.makeReturn(context, reshapedExpr));
        if (replacer != null) {
            replacer.replace(newStatements, true);
        }
        return newStatements;
    }

    public enum ACTION {
        NO_OP, RESHAPE_STATEMENT, RESHAPE_AWAIT, RESHAPE_TRY
    }

    interface StatementsReplacer {
        void replace(List<JCTree.JCStatement> replaces, boolean attr);
    }

    private StatementsReplacer blockStatementsReplacer(JCTree.JCBlock block) {
        return (replaces, attr) -> {
            block.stats = replaces;
            if (attr) {
                JavacUtils.attrStat(context, block);
            }
        };
    }

    private StatementsReplacer caseStatementsReplacer(JCTree.JCCase jcCase) {
        return (replaces, attr) -> {
            jcCase.stats = replaces;
            if (attr) {
                JavacUtils.attrStat(context, jcCase);
            }
        };
    }

    private JCTree.JCStatement toSingleStatement(List<JCTree.JCStatement> statements) {
        if (statements == null || statements.isEmpty()) {
            return null;
        } else if (statements.size() == 1) {
            return statements.head;
        } else {
            return JavacUtils.makeBlock(context, statements);
        }
    }

    private StatementsReplacer ifThenStatementsReplacer(JCTree.JCIf jcIf) {
        return (replaces, attr) -> {
            jcIf.thenpart = toSingleStatement(replaces);
            if (attr) {
                JavacUtils.attrStat(context, jcIf.thenpart);
            }
        };
    }

    private StatementsReplacer toIfElseStatementsReplacer(JCTree.JCIf jcIf) {
        return (replaces, attr) -> {
            jcIf.elsepart = toSingleStatement(replaces);
            if (attr) {
                JavacUtils.attrStat(context, jcIf.elsepart);
            }
        };
    }

    private StatementsReplacer toForeachBodyStatementsReplacer(JCTree.JCEnhancedForLoop jcEnhancedForLoop) {
        return (replaces, attr) -> {
            jcEnhancedForLoop.body = toSingleStatement(replaces);
            if (attr) {
                JavacUtils.attrStat(context, jcEnhancedForLoop.body);
            }
        };
    }

    private StatementsReplacer toWhileBodyStatementsReplacer(JCTree.JCWhileLoop loop) {
        return (replaces, attr) -> {
            loop.body = toSingleStatement(replaces);
            if (attr) {
                JavacUtils.attrStat(context, loop.body);
            }
        };
    }

    private StatementsReplacer toDoWhileBodyStatementsReplacer(JCTree.JCDoWhileLoop loop) {
        return (replaces, attr) -> {
            loop.body = toSingleStatement(replaces);
            if (attr) {
                JavacUtils.attrStat(context, loop.body);
            }
        };
    }

    private StatementsReplacer prependStatementsReplacer(StatementsReplacer replacer, List<JCTree.JCStatement> preStats) {
        if (replacer == null) return null;
        return (replaces, attr) -> {
            if (replaces != null) {
                replacer.replace(replaces.prependList(preStats), attr);
            } else {
                replacer.replace(preStats, attr);
            }
        };
    }
}
