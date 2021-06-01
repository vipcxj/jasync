package io.github.vipcxj.jasync.core.javac;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import io.github.vipcxj.jasync.spec.functional.VoidPromiseFunction;
import io.github.vipcxj.jasync.spec.functional.VoidPromiseSupplier;
import io.github.vipcxj.jasync.spec.helpers.*;

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

    public static JCTree.JCExpression createQualifiedIdent(TreeMaker treeMaker, Names names, String qName) {
        int index = qName.lastIndexOf('.');
        if (index == -1) {
            return treeMaker.Ident(names.fromString(qName));
        } else {
            return treeMaker.Select(
                    createQualifiedIdent(treeMaker, names, qName.substring(0, index)),
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
            IJAsyncContext context,
            JCTree.JCBlock thenBlock,
            List<JCTree.JCCatch> catchBlocks,
            JCTree.JCBlock finallyBlock,
            List<JCTree.JCStatement> otherStatements
    ) {
        TreeMaker treeMaker = context.getTreeMaker();
        Names names = context.getNames();
        int prePos = treeMaker.pos;
        JCTree.JCMethodInvocation result = treeMaker.at(thenBlock).Apply(
                List.nil(),
                JavacUtils.createQualifiedIdent(treeMaker, names, Constants.PROMISE_DEFER_VOID),
                List.of(
                        JavacUtils.createVoidPromiseSupplier(
                                context,
                                forceBlockReturn(
                                        treeMaker,
                                        thenBlock
                                )
                        )
                )
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
                        result = treeMaker.at(jcCatch).Apply(
                                null,
                                treeMaker.Select(result, names.fromString("doCatch")),
                                List.of(
                                        treeMaker.Select(alternative, names.fromString("class")),
                                        treeMaker.Lambda(
                                                List.of(param),
                                                forceBlockReturn(
                                                        treeMaker,
                                                        new TreeCopier<>(treeMaker).copy(jcCatch.body)
                                                )
                                        )
                                )
                        );
                    }

                } else {
                    result = treeMaker.at(jcCatch).Apply(
                            null,
                            treeMaker.Select(result, names.fromString("doCatch")),
                            List.of(
                                    exType == null ? createQualifiedIdent(treeMaker, names, "java.lang.Throwable.class") : treeMaker.Select(exType, names.fromString("class")),
                                    treeMaker.Lambda(
                                            List.of(jcCatch.param),
                                            forceBlockReturn(
                                                    treeMaker,
                                                    jcCatch.body
                                            )
                                    )
                            )
                    );
                }
            }
        }
        if (finallyBlock != null) {
            result = treeMaker.at(finallyBlock).Apply(
                    null,
                    treeMaker.Select(result, names.fromString("doFinally")),
                    List.of(treeMaker.Lambda(
                            List.nil(),
                            forceBlockReturn(
                                    treeMaker,
                                    finallyBlock
                            )
                    ))
            );
        }
        if (otherStatements != null && !otherStatements.isEmpty()) {
            result = treeMaker.at(otherStatements.head).Apply(
                    null,
                    treeMaker.Select(result, names.fromString(Constants.THEN_VOID)),
                    List.of(
                            createVoidPromiseSupplier(
                                    context,
                                    JavacUtils.forceBlockReturn(
                                            treeMaker,
                                            treeMaker.Block(
                                                    0L,
                                                    otherStatements
                                            )
                                    )
                            )
                    )
            );
        }
        treeMaker.pos = prePos;
        return result;
    }

    public static JCTree.JCLiteral createNull(TreeMaker treeMaker) {
        return treeMaker.Literal(TypeTag.BOT, null);
    }

    public static JCTree.JCBlock forceBlockReturn(TreeMaker treeMaker, JCTree.JCBlock block) {
        Boolean returned = ReturnScanner.scanBlock(block);
        if (!Boolean.TRUE.equals(returned)) {
            int prePos = treeMaker.pos;
            if (block.stats.isEmpty()) {
                treeMaker.pos = block.pos;
            } else {
                treeMaker.pos = block.stats.last().pos;
            }
            block.stats = block.stats.append(
                    treeMaker.Return(createNull(treeMaker))
            );
            treeMaker.pos = prePos;
        }
        return block;
    }

    public static JCTree.JCMethodInvocation appendCatchReturn(TreeMaker treeMaker, Names names, JCTree.JCExpression expression) {
        int prePos = treeMaker.pos;
        treeMaker.pos = expression.pos;
        JCTree.JCMethodInvocation catchReturn = treeMaker.Apply(
                null,
                treeMaker.Select(
                        expression,
                        names.fromString("catchReturn")
                ),
                List.nil()
        );
        treeMaker.pos = prePos;
        return catchReturn;
    }

    public static JCTree.JCExpression createVoidPromiseSupplier(IJAsyncContext context, JCTree.JCBlock body) {
        TreeMaker treeMaker = context.getTreeMaker();
        Names names = context.getNames();
        int prePos = treeMaker.pos;
        JCTree.JCExpression expression = treeMaker.NewClass(
                null,
                List.nil(),
                createQualifiedIdent(treeMaker, names, VoidPromiseSupplier.class.getCanonicalName()),
                List.nil(),
                treeMaker.ClassDef(
                        treeMaker.Modifiers(0),
                        names.fromString(""),
                        List.nil(),
                        null,
                        List.nil(),
                        List.of(treeMaker.MethodDef(
                                treeMaker.Modifiers(
                                        Flags.PUBLIC,
                                        List.of(treeMaker.Annotation(
                                                treeMaker.Ident(names.fromString("Override")),
                                                List.nil()
                                        ))
                                ),
                                names.fromString("get"),
                                treeMaker.TypeApply(
                                        treeMaker.Ident(names.fromString("Promise")),
                                        List.of(treeMaker.Ident(names.fromString("Void")))
                                ),
                                List.nil(),
                                null,
                                List.nil(),
                                List.of(createQualifiedIdent(treeMaker, names, Throwable.class.getCanonicalName())),
                                body,
                                null
                        ))
                )
        );
        treeMaker.pos = prePos;
        return expression;
    }

    public static JCTree.JCExpression createVoidPromiseFunction(IJAsyncContext context, JCTree.JCBlock body, Type type, String var) {
        TreeMaker treeMaker = context.getTreeMaker();
        Names names = context.getNames();
        int prePos = treeMaker.pos;
        JCTree.JCExpression expression = treeMaker.NewClass(
                null,
                List.nil(),
                treeMaker.TypeApply(
                        createQualifiedIdent(treeMaker, names, VoidPromiseFunction.class.getCanonicalName()),
                        List.of(treeMaker.Type(type))
                ),
                List.nil(),
                treeMaker.ClassDef(
                        treeMaker.Modifiers(0),
                        names.fromString(""),
                        List.nil(),
                        null,
                        List.nil(),
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
                                        List.of(treeMaker.Ident(names.fromString("Void")))
                                ),
                                List.nil(),
                                null,
                                List.of(treeMaker.VarDef(
                                        treeMaker.Modifiers(8589934592L),
                                        names.fromString(var),
                                        treeMaker.Type(type),
                                        null
                                )),
                                List.of(createQualifiedIdent(treeMaker, names, Throwable.class.getCanonicalName())),
                                body,
                                null
                        ))
                )
        );
        treeMaker.pos = prePos;
        return expression;
    }

    public static boolean equalSymbol(Symbol.VarSymbol symbol1, Symbol.VarSymbol symbol2) {
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
        byte a = 1;
        switch (type.getTag()) {
            case BYTE:
                return createQualifiedIdent(maker, names, ByteReference.class.getCanonicalName());
            case CHAR:
                return createQualifiedIdent(maker, names, CharReference.class.getCanonicalName());
            case SHORT:
                return createQualifiedIdent(maker, names, ShortReference.class.getCanonicalName());
            case INT:
                return createQualifiedIdent(maker, names, IntReference.class.getCanonicalName());
            case LONG:
                return createQualifiedIdent(maker, names, LongReference.class.getCanonicalName());
            case FLOAT:
                return createQualifiedIdent(maker, names, FloatReference.class.getCanonicalName());
            case DOUBLE:
                return createQualifiedIdent(maker, names, DoubleReference.class.getCanonicalName());
            case BOOLEAN:
                return createQualifiedIdent(maker, names, BooleanReference.class.getCanonicalName());
            default:
                return maker.TypeApply(
                        createQualifiedIdent(maker, names, ObjectReference.class.getCanonicalName()),
                        List.of(maker.Type(type))
                );
        }
    }

    private static JCTree.JCExpression createPrimiteReferenceInit(TreeMaker maker, Names names, String type, Name var) {
        return maker.NewClass(
                null,
                List.nil(),
                createQualifiedIdent(maker, names, type),
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
        byte a = 1;
        switch (type.getTag()) {
            case BYTE:
                return createPrimiteReferenceInit(maker, names, ByteReference.class.getCanonicalName(), var);
            case CHAR:
                return createPrimiteReferenceInit(maker, names, CharReference.class.getCanonicalName(), var);
            case SHORT:
                return createPrimiteReferenceInit(maker, names, ShortReference.class.getCanonicalName(), var);
            case INT:
                return createPrimiteReferenceInit(maker, names, IntReference.class.getCanonicalName(), var);
            case LONG:
                return createPrimiteReferenceInit(maker, names, LongReference.class.getCanonicalName(), var);
            case FLOAT:
                return createPrimiteReferenceInit(maker, names, FloatReference.class.getCanonicalName(), var);
            case DOUBLE:
                return createPrimiteReferenceInit(maker, names, DoubleReference.class.getCanonicalName(), var);
            case BOOLEAN:
                return createPrimiteReferenceInit(maker, names, BooleanReference.class.getCanonicalName(), var);
            default: {
                int flag = getFlag(type);
                JCTree.JCLiteral flagArg = maker.Literal(TypeTag.INT, flag);
                return maker.NewClass(
                        null,
                        List.of(maker.Type(type)),
                        createQualifiedIdent(maker, names, ObjectReference.class.getCanonicalName()),
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
}
