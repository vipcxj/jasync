package io.github.vipcxj.jasync.core.javac;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.JavacScope;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.*;
import io.github.vipcxj.jasync.core.javac.model.TreeFactory;
import io.github.vipcxj.jasync.core.javac.model.VarInfo;
import io.github.vipcxj.jasync.core.javac.model.VarKey;
import io.github.vipcxj.jasync.core.javac.model.VarUseState;
import io.github.vipcxj.jasync.core.javac.visitor.ReturnScanner;
import io.github.vipcxj.jasync.core.javac.visitor.TypeCalculator;
import io.github.vipcxj.jasync.spec.Promise;
import io.github.vipcxj.jasync.spec.functional.*;
import io.github.vipcxj.jasync.runtime.helpers.*;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class JavacUtils {

    public static <I, O> List<O> mapList(List<I> list, Function<I, O> mapper) {
        ListBuffer<O> listBuffer = new ListBuffer<>();
        for (I el : list) {
            listBuffer.append(mapper.apply(el));
        }
        return listBuffer.toList();
    }

    public static JCTree.JCExpression makeQualifiedIdent(IJAsyncContext context, String qName) {
        return makeQualifiedIdent(context.getTreeMaker(), context.getNames(), qName);
    }

    public static JCTree.JCExpression makeQualifiedIdent(TreeMaker treeMaker, Names names, String qName) {
        int index = qName.lastIndexOf('.');
        if (index == -1) {
            return treeMaker.Ident(names.fromString(qName));
        } else {
            return treeMaker.Select(
                    makeQualifiedIdent(treeMaker, names, qName.substring(0, index)),
                    names.fromString(qName.substring(index + 1))
            );
        }
    }

    public static String getQualifiedName(JCTree.JCFieldAccess fieldAccess) {
        JCTree.JCExpression expression = fieldAccess.getExpression();
        if (expression instanceof JCTree.JCIdent) {
            return ((JCTree.JCIdent) expression).getName().toString() + "." + fieldAccess.getIdentifier().toString();
        } else if (expression instanceof JCTree.JCFieldAccess) {
            return getQualifiedName((JCTree.JCFieldAccess) expression) + "." + fieldAccess.getIdentifier().toString();
        } else {
            return null;
        }
    }

    public static JCTree.JCMethodInvocation createPromiseThen(
            IJAsyncCuContext context,
            JCTree.JCBlock thenBlock,
            List<JCTree.JCCatch> catchBlocks,
            JCTree.JCBlock finallyBlock,
            List<JCTree.JCStatement> otherStatements
    ) {
        TreeMaker treeMaker = context.getTreeMaker();
        Names names = context.getNames();
        int prePos = treeMaker.pos;
        try {
            JCTree.JCExpression thenSupplier = JavacUtils.makeVoidPromiseSupplier(
                    context,
                    thenBlock
            );
            JCTree.JCMethodInvocation result = makeApply(
                    context,
                    Constants.JASYNC_DEFER_VOID,
                    List.of(thenSupplier)
            );
            if (catchBlocks != null) {
                for (JCTree.JCCatch jcCatch : catchBlocks) {
                    JCTree.JCExpression exType = jcCatch.param.vartype;
                    if (exType != null && exType.getKind() == Tree.Kind.UNION_TYPE) {
                        JCTree.JCTypeUnion typeUnion = (JCTree.JCTypeUnion) exType;
                        for (JCTree.JCExpression alternative : typeUnion.alternatives) {
                            JCTree.JCVariableDecl param = treeMaker.at(jcCatch.param).VarDef(
                                    treeMaker.Modifiers(Flags.PARAMETER),
                                    jcCatch.param.name,
                                    null,
                                    null
                            );
                            result = makeApply(
                                    context,
                                    "doCatch",
                                    List.of(
                                            treeMaker.at(alternative).Select(alternative, names.fromString("class")),
                                            treeMaker.Lambda(
                                                    List.of(param),
                                                    forceBlockReturn(
                                                            context,
                                                            new TreeCopier<>(treeMaker).copy(jcCatch.body)
                                                    )
                                            )
                                    ),
                                    result
                            );
                        }

                    } else {
                        treeMaker.at(jcCatch);
                        result = makeApply(
                                context,
                                "doCatch",
                                List.of(
                                        exType == null ? makeQualifiedIdent(treeMaker, names, "java.lang.Throwable.class") : treeMaker.Select(exType, names.fromString("class")),
                                        treeMaker.Lambda(
                                                List.of(jcCatch.param),
                                                forceBlockReturn(
                                                        context,
                                                        jcCatch.body
                                                )
                                        )
                                ),
                                result
                        );
                    }
                }
            }
            if (finallyBlock != null) {
                JCTree.JCBlock block = forceBlockReturn(
                        context,
                        finallyBlock
                );
                result = makeApply(
                        context,
                        "doFinally",
                        List.of(makeVoidPromiseSupplier(context, block)),
                        result
                );
            }
            if (otherStatements != null && !otherStatements.isEmpty()) {
                result = makeApply(
                        context,
                        Constants.THEN_VOID,
                        List.of(
                                makeVoidPromiseSupplier(
                                        context,
                                        makeBlock(context, otherStatements)
                                )
                        ),
                        result
                );
            }
            return result;
        } finally {
            treeMaker.pos = prePos;
        }
    }

    public static JCTree.JCLiteral makeNull(IJAsyncContext context) {
        TreeMaker treeMaker = context.getTreeMaker();
        return treeMaker.Literal(TypeTag.BOT, null).setType(context.getSymbols().botType);
    }

    public static JCTree.JCBlock forceBlockReturn(IJAsyncCuContext context, JCTree.JCBlock block) {
        Boolean returned = ReturnScanner.scanBlock(block);
        if (!Boolean.TRUE.equals(returned)) {
            TreeMaker treeMaker = context.getTreeMaker();
            int prePos = treeMaker.pos;
            try {
                if (block.stats.isEmpty()) {
                    treeMaker.pos = block.pos;
                } else {
                    treeMaker.pos = getEndPos(context, block.stats.last());
                }
                block.stats = block.stats.append(
                        makeReturn(context, makeNull(context))
                );
            } finally {
                treeMaker.pos = prePos;
            }
        }
        return block;
    }

    public static JCTree.JCMethodInvocation appendCatchReturn(IJAsyncCuContext context, JCTree.JCExpression expression) {
        TreeMaker treeMaker = context.getTreeMaker();
        int prePos = treeMaker.pos;
        try {
            return makeApply(
                    context,
                    "catchReturn",
                    List.nil(),
                    expression
            );
        } finally {
            treeMaker.pos = prePos;
        }
    }

    public static JCTree.JCReturn makeReturn(IJAsyncCuContext context, JCTree.JCExpression expression) {
        TreeMaker maker = context.getTreeMaker();
        return wrapPos(context, maker.Return(expression), expression);
    }

    public static JCTree.JCMethodInvocation makeApply(IJAsyncCuContext context, String methodName) {
        return makeApply(context, methodName, List.nil(), null, List.nil());
    }

    public static JCTree.JCMethodInvocation makeApply(IJAsyncCuContext context, String methodName, List<JCTree.JCExpression> args) {
        return makeApply(context, methodName, args, null, List.nil());
    }

    public static JCTree.JCMethodInvocation makeApply(IJAsyncCuContext context, String methodName, List<JCTree.JCExpression> args, JCTree.JCExpression parent) {
        return makeApply(context, methodName, args, parent, List.nil());
    }

    public static JCTree.JCMethodInvocation makeApply(IJAsyncCuContext context, String methodName, List<JCTree.JCExpression> args, JCTree.JCExpression parent, List<JCTree.JCExpression> typeArgs) {
        if (methodName == null || methodName.isEmpty()) {
            throw new IllegalArgumentException("Invalid method name: " + methodName + ".");
        }
        TreeMaker maker = context.getTreeMaker();
        Names names = context.getNames();
        int prePos = maker.pos;
        try {
            JCTree.JCExpression fn;
            if (parent != null) {
                if (methodName.indexOf('.') != -1) {
                    throw new IllegalArgumentException("With parent provided, the qualified method name is not supported: " + methodName + ".");
                }
                maker.pos = getEndPos(context, parent);
                fn = maker.Select(parent, names.fromString(methodName));
            } else {
                if (args != null && !args.isEmpty()) {
                    maker.pos = args.head.getStartPosition();
                }
                fn = makeQualifiedIdent(context, methodName);
            }
            JCTree.JCMethodInvocation result = maker.Apply(typeArgs, fn, args);
            JCTree.JCCompilationUnit jcu = getJCCompilationUnitTree(context);
            if (jcu != null && jcu.endPositions != null) {
                int endPos = maker.pos;
                if (args != null && !args.isEmpty()) {
                    endPos = getEndPos(context, args.last());
                }
                jcu.endPositions.storeEnd(result, endPos);
            }
            return result;
        } finally {
            maker.pos = prePos;
        }
    }

    public static JCTree.JCExpression makeSupplier(
            IJAsyncCuContext context,
            JCTree.JCBlock body,
            TreeFactory<JCTree.JCExpression> baseClassMaker,
            TreeFactory<JCTree.JCExpression> typeMaker,
            String method
    ) {
        TreeMaker treeMaker = context.getTreeMaker();
        Names names = context.getNames();
        int prePos = treeMaker.pos;
        try {
            return wrapPos(context, treeMaker.at(body).NewClass(
                    null,
                    List.nil(),
                    baseClassMaker.create(context),
                    List.nil(),
                    treeMaker.AnonymousClassDef(
                            treeMaker.Modifiers(0),
                            List.of(treeMaker.MethodDef(
                                    treeMaker.Modifiers(
                                            Flags.PUBLIC,
                                            List.of(treeMaker.Annotation(
                                                    treeMaker.Ident(names.fromString("Override")),
                                                    List.nil()
                                            ))
                                    ),
                                    names.fromString(method),
                                    typeMaker.create(context),
                                    List.nil(),
                                    null,
                                    List.nil(),
                                    List.of(makeQualifiedIdent(treeMaker, names, Throwable.class.getCanonicalName())),
                                    forceBlockReturn(context, body),
                                    null
                            ))
                    )
            ), body);
        } finally {
            treeMaker.pos = prePos;
        }
    }

    public static JCTree.JCExpression makeBooleanSupplier(IJAsyncCuContext context, JCTree.JCExpression expr) {
        return makeSupplier(
                context,
                JavacUtils.makeBlock(context, JavacUtils.makeReturn(context, expr)),
                ctx -> JavacUtils.makeQualifiedIdent(ctx, BooleanSupplier.class.getCanonicalName()),
                ctx -> ctx.getTreeMaker().Type(ctx.getSymbols().booleanType),
                "getAsBoolean"
        );
    }

    public static JCTree.JCExpression makeBooleanPromiseSupplier(IJAsyncCuContext context, JCTree.JCExpression expr) {
        return makeSupplier(
                context,
                JavacUtils.makeBlock(context, JavacUtils.makeReturn(context, expr)),
                ctx -> ctx.getTreeMaker().TypeApply(
                        JavacUtils.makeQualifiedIdent(ctx, PromiseSupplier.class.getCanonicalName()),
                        List.of(
                                context.getTreeMaker().Type(
                                        context.getTypes().boxedTypeOrType(context.getSymbols().booleanType)
                                )
                        )
                ),
                ctx -> ctx.getTreeMaker().TypeApply(
                        JavacUtils.makeQualifiedIdent(ctx, Promise.class.getCanonicalName()),
                        List.of(JavacUtils.makeQualifiedIdent(ctx, Boolean.class.getCanonicalName()))
                ),
                "get"
        );
    }

    public static JCTree.JCExpression makeVoidPromiseSupplier(IJAsyncCuContext context, JCTree.JCBlock body) {
        return makeSupplier(
                context,
                body,
                ctx -> makeQualifiedIdent(ctx, VoidPromiseSupplier.class.getCanonicalName()),
                ctx -> ctx.getTreeMaker().TypeApply(
                        ctx.getTreeMaker().Ident(ctx.getNames().fromString("Promise")),
                        List.of(ctx.getTreeMaker().Ident(ctx.getNames().fromString("Void")))
                ),
                "get"
        );
    }

    private static JCTree.JCExpression makePromiseFunctionType(IJAsyncCuContext context, Type typeA, Type typeB) {
        TreeMaker maker = context.getTreeMaker();
        Names names = context.getNames();
        if (typeB == null) {
            switch (typeA.getTag()) {
                case BOOLEAN:
                    return makeQualifiedIdent(maker, names, BooleanVoidPromiseFunction.class.getCanonicalName());
                case CHAR:
                    return makeQualifiedIdent(maker, names, CharVoidPromiseFunction.class.getCanonicalName());
                case BYTE:
                    return makeQualifiedIdent(maker, names, ByteVoidPromiseFunction.class.getCanonicalName());
                case SHORT:
                    return makeQualifiedIdent(maker, names, ShortVoidPromiseFunction.class.getCanonicalName());
                case INT:
                    return makeQualifiedIdent(maker, names, IntVoidPromiseFunction.class.getCanonicalName());
                case LONG:
                    return makeQualifiedIdent(maker, names, LongVoidPromiseFunction.class.getCanonicalName());
                case FLOAT:
                    return makeQualifiedIdent(maker, names, FloatVoidPromiseFunction.class.getCanonicalName());
                case DOUBLE:
                    return makeQualifiedIdent(maker, names, DoubleVoidPromiseFunction.class.getCanonicalName());
                default:
                    return maker.TypeApply(
                            makeQualifiedIdent(maker, names, VoidPromiseFunction.class.getCanonicalName()),
                            List.of(maker.Type(typeA))
                    );
            }
        } else {
            return maker.TypeApply(
                    makeQualifiedIdent(maker, names, PromiseFunction.class.getCanonicalName()),
                    List.of(
                            maker.Type(typeA),
                            maker.Type(typeB)
                    )
            );
        }
    }

    public static JCTree.JCExpression makePromiseFunction(IJAsyncCuContext context, JCTree.JCBlock body, Type typeA, Type typeB, String var) {
        TreeMaker treeMaker = context.getTreeMaker();
        Names names = context.getNames();
        int prePos = treeMaker.pos;
        try {
            return wrapPos(context, treeMaker.at(body).NewClass(
                    null,
                    List.nil(),
                    makePromiseFunctionType(context, typeA, typeB),
                    List.nil(),
                    treeMaker.AnonymousClassDef(
                            treeMaker.Modifiers(0),
                            List.of(treeMaker.MethodDef(
                                    treeMaker.Modifiers(
                                            Flags.PUBLIC,
                                            List.of(treeMaker.Annotation(
                                                    treeMaker.Ident(names.fromString("Override")),
                                                    List.nil()
                                            ))
                                    ),
                                    names.fromString("apply"),
                                    treeMaker.TypeApply(
                                            treeMaker.Ident(names.fromString("Promise")),
                                            List.of(typeB != null ? treeMaker.Type(typeB) : treeMaker.Ident(names.fromString("Void")))
                                    ),
                                    List.nil(),
                                    null,
                                    List.of(treeMaker.VarDef(
                                            treeMaker.Modifiers(8589934592L),
                                            names.fromString(var),
                                            treeMaker.Type(typeA),
                                            null
                                    )),
                                    List.of(makeQualifiedIdent(treeMaker, names, Throwable.class.getCanonicalName())),
                                    forceBlockReturn(context, body),
                                    null
                            ))
                    )
            ), body);
        } finally {
            treeMaker.pos = prePos;
        }
    }

    public static JCTree.JCExpression makeVoidPromiseFunction(IJAsyncCuContext context, JCTree.JCBlock body, Type type, String var) {
        return makePromiseFunction(
                context,
                body,
                type,
                null,
                var
        );
    }

    public static JCTree.JCBlock makeBlock(IJAsyncCuContext context, List<JCTree.JCStatement> statements) {
        TreeMaker treeMaker = context.getTreeMaker();
        return wrapPos(context, treeMaker.Block(0L, statements), statements);
    }

    public static JCTree.JCBlock makeBlock(IJAsyncCuContext context, JCTree.JCStatement statement) {
        if (statement instanceof JCTree.JCBlock) return (JCTree.JCBlock) statement;
        return makeBlock(context, List.of(statement));
    }

    public static JCTree.JCExpression makePromise(IJAsyncCuContext context, JCTree.JCExpression expression) {
        return makeApply(
                context,
                Constants.JASYNC_JUST,
                List.of(expression)
        );
    }

    public static JCTree.JCBlock shallowCopyBlock(IJAsyncCuContext context, JCTree.JCBlock block) {
        TreeMaker treeMaker = context.getTreeMaker();
        return wrapPos(context, treeMaker.Block(0L, block.stats), block);
    }

    public static JCTree.JCExpressionStatement makeExprStat(IJAsyncCuContext context, JCTree.JCExpression expression) {
        TreeMaker maker = context.getTreeMaker();
        return wrapPos(context, maker.Exec(expression), expression);
    }

    public static JCTree.JCStatement makeWhileFromForLoop(IJAsyncCuContext context, JCTree.JCForLoop forLoop) {
        TreeMaker maker = context.getTreeMaker();
        int prePos = maker.pos;
        try {
            JCTree.JCStatement body = forLoop.body;
            if (forLoop.step != null && !forLoop.step.isEmpty()) {
                if (body instanceof JCTree.JCBlock) {
                    JCTree.JCBlock block = (JCTree.JCBlock) body;
                    ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();
                    if (block.stats != null) {
                        statements.appendList(block.stats);
                    }
                    for (JCTree.JCExpressionStatement statement : forLoop.step) {
                        statements.append(statement);
                    }
                    block.stats = statements.toList();
                } else {
                    ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();
                    statements.append(body);
                    for (JCTree.JCExpressionStatement statement : forLoop.step) {
                        statements.append(statement);
                    }
                    body = makeBlock(context, statements.toList());
                }
            }
            maker.at(forLoop);
            atEndPos(context, forLoop.init);
            atPosIfValid(context, getStartPos(forLoop.cond));
            JCTree.JCWhileLoop whileLoop = maker.WhileLoop(
                    forLoop.cond,
                    body
            );
            JCTree.JCCompilationUnit jcu = getJCCompilationUnitTree(context);
            if (jcu != null && jcu.endPositions != null) {
                jcu.endPositions.replaceTree(forLoop, whileLoop);
            }
            if (forLoop.init != null && !forLoop.init.isEmpty()) {
                ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();
                for (JCTree.JCStatement statement : forLoop.init) {
                    statements.append(statement);
                }
                statements.append(whileLoop);
                return makeBlock(context, statements.toList());
            } else {
                whileLoop.pos = forLoop.pos;
                return whileLoop;
            }
        } finally {
            maker.pos = prePos;
        }
    }

    public static JCTree.JCCompilationUnit getJCCompilationUnitTree(IJAsyncCuContext context) {
        CompilationUnitTree cu = context.getCompilationUnitTree();
        JCTree.JCCompilationUnit jcu = null;
        if (cu instanceof JCTree.JCCompilationUnit) {
            jcu = (JCTree.JCCompilationUnit) cu;
        }
        return jcu;
    }

    public static int getStartPos(JCTree tree) {
        if (tree == null) return -1;
        return tree.getStartPosition();
    }

    public static int getStartPos(List<? extends JCTree> trees) {
        if (trees == null || trees.isEmpty()) {
            return -1;
        }
        for (JCTree tree : trees) {
            if (tree != null) {
                int position = tree.getStartPosition();
                if (position >= 0) {
                    return position;
                }
            }
        }
        return -1;
    }

    public static int getEndPos(IJAsyncCuContext context, JCTree tree) {
        if (tree == null) return -1;
        JCTree.JCCompilationUnit jcu = getJCCompilationUnitTree(context);
        int endPos = tree.getEndPosition(jcu != null ? jcu.endPositions : null);
        return Math.max(endPos, tree.pos);
    }

    public static int getEndPos(IJAsyncCuContext context, List<? extends JCTree> trees) {
        if (trees == null || trees.isEmpty()) {
            return -1;
        }
        int position = -1;
        for (JCTree tree : trees) {
            if (tree != null) {
                int pos = getEndPos(context, tree);
                if (pos >= 0) {
                    position = pos;
                }
            }
        }
        return position;
    }

    public static <T extends JCTree> T wrapPos(IJAsyncCuContext context, T tree, JCTree target, boolean replace) {
        if (target == null) return tree;
        tree.pos = target.getStartPosition();
        JCTree.JCCompilationUnit jcu = getJCCompilationUnitTree(context);
        if (jcu != null && jcu.endPositions != null) {
            if (replace) {
                jcu.endPositions.replaceTree(target, tree);
            } else {
                int endPos = getEndPos(context, target);
                jcu.endPositions.storeEnd(tree, endPos);
            }
        }
        return tree;
    }

    public static <T extends JCTree> T wrapPos(IJAsyncCuContext context, T tree, JCTree target) {
        return wrapPos(context, tree, target, false);
    }

    public static <T extends JCTree> T wrapPos(IJAsyncCuContext context, T tree, List<? extends JCTree> targets) {
        if (targets == null || targets.isEmpty()) return tree;
        tree.pos = targets.head.getStartPosition();
        JCTree.JCCompilationUnit jcu = getJCCompilationUnitTree(context);
        if (jcu != null && jcu.endPositions != null) {
            jcu.endPositions.storeEnd(tree, getEndPos(context, targets.last()));
        }
        return tree;
    }

    public static boolean equalSymbol(Symbol.VarSymbol symbol1, Symbol.VarSymbol symbol2) {
        if (symbol1 == null) {
            return symbol2 == null;
        }
        if (symbol2 == null) {
            return false;
        }
        if (symbol1.pos != symbol2.pos) {
            return false;
        }
        if (symbol1.getKind() != symbol2.getKind()) {
            return false;
        }
        Name simpleName1 = symbol1.getSimpleName();
        Name simpleName2 = symbol2.getSimpleName();
        return simpleName1 != null && simpleName2 != null && Objects.equals(simpleName1.toString(), simpleName2.toString());
    }

    private static JCTree.JCExpression createReferenceType(TreeMaker maker, Names names, Type type) {
        if (type == null) return null;
        switch (type.getTag()) {
            case BYTE:
                return makeQualifiedIdent(maker, names, ByteReference.class.getCanonicalName());
            case CHAR:
                return makeQualifiedIdent(maker, names, CharReference.class.getCanonicalName());
            case SHORT:
                return makeQualifiedIdent(maker, names, ShortReference.class.getCanonicalName());
            case INT:
                return makeQualifiedIdent(maker, names, IntReference.class.getCanonicalName());
            case LONG:
                return makeQualifiedIdent(maker, names, LongReference.class.getCanonicalName());
            case FLOAT:
                return makeQualifiedIdent(maker, names, FloatReference.class.getCanonicalName());
            case DOUBLE:
                return makeQualifiedIdent(maker, names, DoubleReference.class.getCanonicalName());
            case BOOLEAN:
                return makeQualifiedIdent(maker, names, BooleanReference.class.getCanonicalName());
            default:
                return maker.TypeApply(
                        makeQualifiedIdent(maker, names, ObjectReference.class.getCanonicalName()),
                        List.of(maker.Type(type))
                );
        }
    }

    private static JCTree.JCExpression createPrimateReferenceInit(TreeMaker maker, Names names, String type, Name var) {
        return maker.NewClass(
                null,
                List.nil(),
                makeQualifiedIdent(maker, names, type),
                var != null ? List.of(maker.Ident(var)) : List.nil(),
                null
        );
    }

    private static int getFlag(Type type) {
        Symbol.TypeSymbol tsym = type.tsym;
        if (tsym == null)
            return ObjectReference.FLAG_OTHER;
        String qName = tsym.getQualifiedName().toString();
        if (Byte.class.getCanonicalName().equals(qName)) {
            return ObjectReference.FLAG_BYTE;
        } else if (Character.class.getCanonicalName().equals(qName)) {
            return ObjectReference.FLAG_CHAR;
        } else if (Short.class.getCanonicalName().equals(qName)) {
            return ObjectReference.FLAG_SHORT;
        } else if (Integer.class.getCanonicalName().equals(qName)) {
            return ObjectReference.FLAG_INT;
        } else if (Long.class.getCanonicalName().equals(qName)) {
            return ObjectReference.FLAG_LONG;
        } else if (Float.class.getCanonicalName().equals(qName)) {
            return ObjectReference.FLAG_FLOAT;
        } else if (Double.class.getCanonicalName().equals(qName)) {
            return ObjectReference.FLAG_DOUBLE;
        } else {
            return ObjectReference.FLAG_OTHER;
        }
    }

    private static JCTree.JCExpression createReferenceInit(TreeMaker maker, Names names, Type type, Name var) {
        if (type == null) return null;
        switch (type.getTag()) {
            case BYTE:
                return createPrimateReferenceInit(maker, names, ByteReference.class.getCanonicalName(), var);
            case CHAR:
                return createPrimateReferenceInit(maker, names, CharReference.class.getCanonicalName(), var);
            case SHORT:
                return createPrimateReferenceInit(maker, names, ShortReference.class.getCanonicalName(), var);
            case INT:
                return createPrimateReferenceInit(maker, names, IntReference.class.getCanonicalName(), var);
            case LONG:
                return createPrimateReferenceInit(maker, names, LongReference.class.getCanonicalName(), var);
            case FLOAT:
                return createPrimateReferenceInit(maker, names, FloatReference.class.getCanonicalName(), var);
            case DOUBLE:
                return createPrimateReferenceInit(maker, names, DoubleReference.class.getCanonicalName(), var);
            case BOOLEAN:
                return createPrimateReferenceInit(maker, names, BooleanReference.class.getCanonicalName(), var);
            default: {
                int flag = getFlag(type);
                JCTree.JCLiteral flagArg = maker.Literal(TypeTag.INT, flag);
                return maker.NewClass(
                        null,
                        List.of(maker.Type(type)),
                        makeQualifiedIdent(maker, names, ObjectReference.class.getCanonicalName()),
                        var != null ? List.of(maker.Ident(var), flagArg) : List.of(flagArg),
                        null
                );
            }
        }
    }

    public static ListBuffer<JCTree.JCStatement> createVarDecls(IJAsyncContext context, Map<VarKey, VarInfo> varData, int pos) {
        ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();
        TreeMaker maker = context.getTreeMaker();
        Names names = context.getNames();
        int prePos = maker.pos;
        maker.pos = pos;
        for (VarInfo info : varData.values()) {
            if (info.getState() == VarUseState.WRITE_BEFORE) {
                String nextVar = context.nextVar();
                Symbol.VarSymbol symbol = info.getSymbol();
                JCTree.JCVariableDecl decl = maker.VarDef(
                        maker.Modifiers(Flags.FINAL),
                        names.fromString(nextVar),
                        maker.Type(symbol.type),
                        maker.Ident(symbol.name)
                );
                info.setNewName(nextVar);
                statements.append(decl);
            } else if (info.getState() == VarUseState.WRITE) {
                String nextVar = context.nextVar();
                Symbol.VarSymbol symbol = info.getSymbol();
                JCTree.JCVariableDecl decl = maker.VarDef(
                        maker.Modifiers(Flags.FINAL),
                        names.fromString(nextVar),
                        createReferenceType(maker, names, symbol.type),
                        createReferenceInit(maker, names, symbol.type, info.isInitialized() ? symbol.name : null)
                );
                info.setNewName(nextVar);
                statements.append(decl);
            }
        }
        maker.pos = prePos;
        return statements;
    }

    public static ListBuffer<JCTree.JCStatement> resumeVarDecls(IJAsyncContext context, Map<VarKey, VarInfo> varData, int pos) {
        ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();
        TreeMaker maker = context.getTreeMaker();
        Names names = context.getNames();
        int prePos = maker.pos;
        maker.pos = pos;
        for (VarInfo info : varData.values()) {
            if (info.getState() == VarUseState.WRITE_BEFORE) {
                Symbol.VarSymbol symbol = info.getSymbol();
                JCTree.JCVariableDecl decl = maker.VarDef(
                        maker.Modifiers(Flags.FINAL),
                        symbol.name,
                        maker.Type(symbol.type),
                        maker.Ident(names.fromString(info.getNewName()))
                );
                statements.append(decl);
            } else if (info.getState() == VarUseState.WRITE) {
                Symbol.VarSymbol symbol = info.getSymbol();
                JCTree.JCVariableDecl decl = maker.VarDef(
                        maker.Modifiers(Flags.FINAL),
                        symbol.name,
                        createReferenceType(maker, names, symbol.type),
                        maker.Ident(names.fromString(info.getNewName()))
                );
                statements.append(decl);
            }
        }
        maker.pos = prePos;
        return statements;
    }

    private static JCTree.JCExpression toEnum(IJAsyncCuContext context, JCTree.JCExpression pat) {
        if (pat instanceof JCTree.JCIdent) {
            TreeMaker treeMaker = context.getTreeMaker();
            int prePos = treeMaker.pos;
            JCTree.JCExpression res = treeMaker.at(pat).Select(treeMaker.Type(getType(context, pat)), ((JCTree.JCIdent) pat).name);
            treeMaker.pos = prePos;
            return res;
        }
        throw new IllegalArgumentException("Unable to resolve the enum value from: " + pat);
    }

    public static JCTree.JCExpression makeCase(IJAsyncCuContext context, int caseType, JCTree.JCExpression pat, JCTree.JCBlock block) {
        TreeMaker maker = context.getTreeMaker();
        int prePos = maker.pos;
        JCTree.JCExpression expression = null;
        switch (caseType) {
            case 1:
                expression = makeApply(
                        context,
                        Constants.INT_CASE_OF,
                        List.of(pat, makeVoidPromiseSupplier(context, block))
                );
                break;
            case 2:
                expression = makeApply(
                        context,
                        Constants.STRING_CASE_OF,
                        List.of(pat, makeVoidPromiseSupplier(context, block))
                );
                break;
            case 3:
                expression = makeApply(
                        context,
                        Constants.ENUM_CASE_OF,
                        List.of(toEnum(context, pat), makeVoidPromiseSupplier(context, block))
                );
                break;
        }
        maker.pos = prePos;
        if (expression == null) {
            throw new IllegalArgumentException("Invalid case type: " + caseType);
        }
        return expression;
    }

    // loop should be processed.
    public static JCTree.JCExpression makeForEach(IJAsyncCuContext context, JCTree.JCEnhancedForLoop loop) {
        if (loop == null) {
            return null;
        }
        Type type = getType(context, loop.var);
        String method;
        switch (type.getTag()) {
            case BYTE:
                method = Constants.JASYNC_DO_FOR_EACH_BYTE;
                break;
            case CHAR:
                method = Constants.JASYNC_DO_FOR_EACH_CHAR;
                break;
            case SHORT:
                method = Constants.JASYNC_DO_FOR_EACH_SHORT;
                break;
            case INT:
                method = Constants.JASYNC_DO_FOR_EACH_INT;
                break;
            case LONG:
                method = Constants.JASYNC_DO_FOR_EACH_LONG;
                break;
            case FLOAT:
                method = Constants.JASYNC_DO_FOR_EACH_FLOAT;
                break;
            case DOUBLE:
                method = Constants.JASYNC_DO_FOR_EACH_DOUBLE;
                break;
            case BOOLEAN:
                method = Constants.JASYNC_DO_FOR_EACH_BOOLEAN;
                break;
            default:
                method = Constants.JASYNC_DO_FOR_EACH_OBJECT;
        }
        return makeApply(
                context,
                method,
                List.of(
                        loop.expr,
                        makeVoidPromiseFunction(
                                context,
                                makeBlock(context, loop.body),
                                type,
                                loop.var.name.toString()
                        )
                )
        );
    }

    public static Type getType(IJAsyncCuContext context, JCTree jcTree) {
        if (jcTree instanceof JCTree.JCExpression || jcTree instanceof JCTree.JCStatement) {
            TypeCalculator calculator = new TypeCalculator();
            jcTree.accept(calculator);
            if (calculator.getType() == null) {
                if (jcTree instanceof JCTree.JCExpression) {
                    attrExpr(context, (JCTree.JCExpression) jcTree);
                } else {
                    attrStat(context, (JCTree.JCStatement) jcTree);
                }
                jcTree.accept(calculator);
            }
            return calculator.getType();
        } else {
            return null;
        }
    }

    public static void attrExpr(IJAsyncCuContext context, JCTree.JCExpression jcTree) {
        Log.DiagnosticHandler discardHandler = new Log.DiscardDiagnosticHandler(context.getLog());
        try {
            JavacScope scope = context.getScope(jcTree);
            if (scope != null) {
                context.getAttr().attribExpr(jcTree, scope.getEnv());
            }
        } finally {
            context.getLog().popDiagnosticHandler(discardHandler);
        }
    }

    public static void attrStat(IJAsyncCuContext context, JCTree.JCStatement jcTree) {
        Log.DiagnosticHandler discardHandler = new Log.DiscardDiagnosticHandler(context.getLog());
        try {
            JavacScope scope = context.getScope(jcTree);
            if (scope != null) {
                context.getAttr().attribStat(jcTree, scope.getEnv());
            }
        } finally {
            context.getLog().popDiagnosticHandler(discardHandler);
        }
    }

    public static void atPosIfValid(IJAsyncCuContext context, int pos) {
        if (pos >= 0) {
            context.getTreeMaker().at(pos);
        }
    }

    public static void atPosIfGreater(IJAsyncCuContext context, int pos) {
        TreeMaker maker = context.getTreeMaker();
        if (pos > maker.pos) {
            maker.pos = pos;
        }
    }

    public static void atStartPos(IJAsyncCuContext context, List<? extends JCTree> trees) {
        atPosIfValid(context, getStartPos(trees));
    }

    public static void atEndPos(IJAsyncCuContext context, List<? extends JCTree> trees) {
        atPosIfValid(context, getEndPos(context, trees));
    }

    public static void atCaseBlockStart(IJAsyncCuContext context, JCTree.JCCase jcCase) {
        TreeMaker maker = context.getTreeMaker();
        int casePosition = jcCase.getStartPosition();
        if (casePosition < 0) {
            return;
        }
        atPosIfGreater(context, casePosition + 1);
        if (jcCase.pat == null) {
            atPosIfGreater(context, casePosition + 8);
        } else {
            atPosIfGreater(context, getEndPos(context, jcCase.pat));
        }
        atPosIfGreater(context, getStartPos(jcCase.stats));
    }
}
