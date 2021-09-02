package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import io.github.vipcxj.jasync.core.javac.Constants;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.context.JAsyncSymbols;
import io.github.vipcxj.jasync.core.javac.model.Frame;
import io.github.vipcxj.jasync.core.javac.translate.TransFrameHolderContext;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;

public class TransSwitchContext
        extends AbstractTransFrameHolderStatementContext<JCTree.JCSwitch>
        implements TransFrameHolderContext<JCTree.JCSwitch> {

    private TranslateContext<?> selectorContext;
    private ListBuffer<TransCaseContext> caseContexts;

    public TransSwitchContext(AnalyzerContext analyzerContext, JCTree.JCSwitch tree) {
        super(analyzerContext, tree);
        awaitContainer = tree.selector;
        this.caseContexts = new ListBuffer<>();
    }

    @Override
    public TransSwitchContext enter() {
        super.enter();
        return this;
    }

    private int caseNum() {
        return tree.cases != null ? tree.cases.size() : 0;
    }

    protected void childContextMustBeCase(TranslateContext<?> child) {
        if (child instanceof TransCaseContext) return;
        throw new IllegalArgumentException("The child context must be case context.");
    }

    @Override
    protected void addNormalChildContext(TranslateContext<?> child) {
        if (selectorContext == null) {
            checkContextTree(child, tree.selector);
            selectorContext = child;
        } else if (caseContexts.size() < caseNum()) {
            JCTree.JCCase jcCase = tree.cases.get(caseContexts.size());
            childContextMustBeCase(child);
            checkContextTree(child, jcCase);
            caseContexts = caseContexts.append((TransCaseContext) child);
        } else {
            throwIfFull();
        }
    }

    @Override
    public void complete() {
        selectorContext.complete();
        if (!hasAwait()) {
            Frame preFrame = analyzerContext.enter(this);
            frame = analyzerContext.currentFrame();
            frame.markOrder();
            try {
                for (TransCaseContext caseContext : caseContexts) {
                    caseContext.complete();
                }
            } finally {
                analyzerContext.exitTo(preFrame);
            }
        } else {
            for (TransCaseContext caseContext : caseContexts) {
                caseContext.complete();
            }
        }
        if (thenContext != null) {
            thenContext.complete();
        }
        complete = true;
    }

    private void checkCases() {
        for (JCTree.JCCase jcCase : tree.cases) {
            if (jcCase.stats != null) {
                if (jcCase.stats.size() != 1 || !(jcCase.stats.head instanceof JCTree.JCBlock)) {
                    throw new IllegalStateException("The case is not normalized: case " + jcCase.pat + ".");
                }
            }
        }
    }

    @Override
    protected JCTree buildTreeWithoutThen(boolean replaceSelf) {
        if (hasAwait()) {
            checkCases();
            TransMethodContext methodContext = getEnclosingMethodContext();
            IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
            JAsyncSymbols symbols = jasyncContext.getJAsyncSymbols();
            TreeMaker maker = jasyncContext.getTreeMaker();
            int prePos = maker.pos;
            try {
                maker.pos = tree.pos;
                ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
                for (TransCaseContext caseContext : caseContexts) {
                    int caseType = JavacUtils.getCaseType(jasyncContext, caseContext.tree);
                    TranslateContext<?> patContext = caseContext.getPatContext();
                    TransBlockContext bodyContext = caseContext.getSingleBodyContext();
                    JCTree.JCMethodDecl methodDecl = methodContext.addVoidPromiseSupplier(bodyContext);
                    JCTree.JCExpression voidPromiseSupplier = methodContext.makeFunctional(bodyContext.getFrame(), Constants.INDY_MAKE_VOID_PROMISE_SUPPLIER, methodDecl);
                    JCTree.JCExpression argExpr;
                    switch (caseType) {
                        case 0: // default
                            argExpr = safeMaker().Apply(
                                    List.nil(),
                                    symbols.makeDefaultCaseOf(),
                                    List.of(voidPromiseSupplier)
                            );
                            break;
                        case 1: // int
                            argExpr = safeMaker().Apply(
                                    List.nil(),
                                    symbols.makeIntCaseOf(),
                                    List.of(
                                            (JCTree.JCExpression) patContext.buildTree(false),
                                            voidPromiseSupplier
                                    )
                            );
                            break;
                        case 2: // String
                            argExpr = safeMaker().Apply(
                                    List.nil(),
                                    symbols.makeStringCaseOf(),
                                    List.of(
                                            (JCTree.JCExpression) patContext.buildTree(false),
                                            voidPromiseSupplier
                                    )
                            );
                            break;
                        case 3: // Enum
                            argExpr = safeMaker().Apply(
                                    List.nil(),
                                    symbols.makeEnumCaseOf(),
                                    List.of(
                                            (JCTree.JCExpression) patContext.buildTree(false),
                                            voidPromiseSupplier
                                    )
                            );
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown case type: " + caseType + ".");
                    }
                    args = args.append(argExpr);
                }
                return JavacUtils.makeReturn(jasyncContext, safeMaker().Apply(
                        List.nil(),
                        symbols.makeJAsyncDoSwitch(),
                        List.of(
                                (JCTree.JCExpression) selectorContext.buildTree(false),
                                safeMaker().Apply(
                                        List.nil(),
                                        symbols.makeCasesOf(),
                                        args.toList()
                                ),
                                makeLabelArg()
                        )
                ));
            } finally {
                maker.pos = prePos;
            }
        } else {
            tree.selector = (JCTree.JCExpression) selectorContext.buildTree(false);
            tree.cases = JavacUtils.mapList(caseContexts.toList(), ctx -> (JCTree.JCCase) ctx.buildTree(false));
            return tree;
        }
    }
}
