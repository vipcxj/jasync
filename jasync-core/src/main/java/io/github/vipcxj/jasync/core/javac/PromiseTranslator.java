package io.github.vipcxj.jasync.core.javac;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacScope;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Iterator;
import java.util.Map;

public class PromiseTranslator extends TreeTranslator implements IJAsyncCuContext {

    private final IJAsyncCuContext context;
    private ACTION action;
    private JCTree.JCExpression unwrap;
    private final String replacedVar;
    private Type awaitType;
    private boolean complete;
    private JCTree.JCStatement toReshape;
    private final boolean reshaped;

    public PromiseTranslator(IJAsyncCuContext jAsyncContext, boolean reshaped) {
        this.context = jAsyncContext;
        this.action = ACTION.NO_OP;
        this.unwrap = null;
        this.replacedVar = context.nextVar();
        this.complete = false;
        this.reshaped = reshaped;
    }

    public PromiseTranslator copy() {
        return new PromiseTranslator(context, reshaped);
    }

    public PromiseTranslator copyReshaped() {
        return new PromiseTranslator(context, true);
    }

    @Override
    public ProcessingEnvironment getEnvironment() {
        return context.getEnvironment();
    }

    @Override
    public Context getContext() {
        return context.getContext();
    }

    @Override
    public TreeMaker getTreeMaker() {
        return context.getTreeMaker();
    }

    @Override
    public Names getNames() {
        return context.getNames();
    }

    @Override
    public JavacTrees getTrees() {
        return context.getTrees();
    }

    @Override
    public String nextVar() {
        return context.nextVar();
    }

    @Override
    public CompilationUnitTree getCompilationUnitTree() {
        return context.getCompilationUnitTree();
    }

    @Override
    public TreePath getPath(JCTree tree) {
        return context.getPath(tree);
    }

    @Override
    public JavacScope getScope(JCTree tree) {
        return context.getScope(tree);
    }

    @Override
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
                        int prePos = getTreeMaker().pos;
                        this.result = getTreeMaker().at(jcMethodInvocation).Ident(getNames().fromString(replacedVar));
                        getTreeMaker().pos = prePos;
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
        List<JCTree.JCStatement> stats = copy().reshapeStatements(jcBlock.stats);
        if (jcBlock.stats != stats) {
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
            JCTree.JCStatement thenPart = copy().reshapeStatement(jcIf.thenpart);
            JCTree.JCStatement elsePart = copy().reshapeStatement(jcIf.elsepart);
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
    public void visitForeachLoop(JCTree.JCEnhancedForLoop jcEnhancedForLoop) {
        this.result = jcEnhancedForLoop;
    }

    @Override
    public void visitForLoop(JCTree.JCForLoop jcForLoop) {
        this.result = jcForLoop;
    }

    @Override
    public void visitWhileLoop(JCTree.JCWhileLoop jcWhileLoop) {
        this.result = jcWhileLoop;
    }

    @Override
    public void visitDoLoop(JCTree.JCDoWhileLoop jcDoWhileLoop) {
        this.result = jcDoWhileLoop;
    }

    @Override
    public void visitTry(JCTree.JCTry jcTry) {
        if (jcTry == toReshape) {
            jcTry.resources = translate(jcTry.resources);
            if (!complete) {
                if (copyReshaped().reshape(jcTry.body) || copyReshaped().visitCatches(jcTry.catchers) || copyReshaped().reshape(jcTry.finalizer)) {
                    this.action = ACTION.RESHAPE_TRY;
                    this.complete = true;
                }
            }
            result = jcTry;
        } else {
            result = copy().reshapeStatement(jcTry);
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
        reshape(methodDecl.body);
    }

    public boolean reshape(JCTree.JCBlock block) {
        if (block == null) return false;
        List<JCTree.JCStatement> stats = reshapeStatements(block.stats);
        if (stats != block.stats) {
            block.stats = stats;
            return true;
        } else {
            return false;
        }
    }

    public boolean visitCatches(List<JCTree.JCCatch> catches) {
        if (catches == null) return false;
        boolean changed = false;
        for (JCTree.JCCatch jcCatch : catches) {
            changed = changed || reshape(jcCatch.body);
        }
        return changed;
    }

    public JCTree.JCStatement reshapeStatement(JCTree.JCStatement statement) {
        if (statement == null) return null;
        List<JCTree.JCStatement> reshapedStatements = reshapeStatements(List.of(statement));
        assert reshapedStatements.size() == 1;
        return reshapedStatements.head;
    }

    public List<JCTree.JCStatement> reshapeStatements(List<JCTree.JCStatement> statements) {
        TreeMaker treeMaker = getTreeMaker();
        Names names = getNames();
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
                    current = statement;
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
                reshapedExpr = treeMaker.at(unwrap).Apply(
                        null,
                        treeMaker.Select(unwrap, names.fromString(Constants.THEN_VOID)),
                        List.nil()
                );
            } else {
                reshapedExpr = treeMaker.at(unwrap).Apply(
                        null,
                        treeMaker.Select(unwrap, names.fromString(Constants.THEN_VOID)),
                        List.of(JavacUtils.createVoidPromiseFunction(
                                context,
                                JavacUtils.forceBlockReturn(
                                        treeMaker,
                                        treeMaker.Block(
                                                0L,
                                                copyReshaped().reshapeStatements(tails)
                                        )
                                ),
                                awaitType,
                                replacedVar
                        ))
                );
            }
            treeMaker.pos = prePos;
        } else if (action == ACTION.RESHAPE_TRY) {
            JCTree.JCTry jcTry = (JCTree.JCTry) current;
            int prePos = treeMaker.pos;
            treeMaker.pos = current.pos;
            reshapedExpr = JavacUtils.createPromiseThen(
                    this,
                    jcTry.body,
                    jcTry.catchers,
                    jcTry.finalizer,
                    tails
            );
            treeMaker.pos = prePos;
        } else if (action == ACTION.RESHAPE_STATEMENT) {
            int prePos = treeMaker.pos;
            reshapedExpr = treeMaker.at(current).Apply(
                    List.nil(),
                    JavacUtils.createQualifiedIdent(treeMaker, names, Constants.PROMISE_DEFER_VOID),
                    List.of(
                            JavacUtils.createVoidPromiseSupplier(
                                    this,
                                    JavacUtils.forceBlockReturn(
                                            treeMaker,
                                            current.getKind() == Tree.Kind.BLOCK ? (JCTree.JCBlock) current : treeMaker.Block(0L, List.of(current))
                                    )
                            )
                    )
            );
            if (!tails.isEmpty()) {
                reshapedExpr = treeMaker.at(tails.head).Apply(
                        null,
                        treeMaker.Select(reshapedExpr, names.fromString(Constants.THEN_VOID)),
                        List.of(
                                JavacUtils.createVoidPromiseSupplier(
                                        this,
                                        JavacUtils.forceBlockReturn(
                                                treeMaker,
                                                treeMaker.Block(
                                                        0L,
                                                        copyReshaped().reshapeStatements(tails)
                                                )
                                        )
                                )
                        )
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
                reshapedExpr = treeMaker.at(reshapedExpr).Apply(
                        List.nil(),
                        JavacUtils.createQualifiedIdent(treeMaker, names, Constants.PROMISE_DEFER_VOID),
                        List.of(JavacUtils.createVoidPromiseSupplier(context, treeMaker.Block(0L, decls2.toList().append(
                                treeMaker.Return(reshapedExpr)
                        ))))
                );
            }
            if (!reshaped) {
                reshapedExpr = JavacUtils.appendCatchReturn(treeMaker, names, reshapedExpr);
            }
        }
        newStatements = heads.append(
                treeMaker.at(reshapedExpr).Return(
                        reshapedExpr
                )
        );
        return newStatements;
    }

    public enum ACTION {
        NO_OP, RESHAPE_STATEMENT, RESHAPE_AWAIT, RESHAPE_TRY
    }
}
