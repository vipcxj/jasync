package io.github.vipcxj.jasync.core.javac.context;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import io.github.vipcxj.jasync.core.javac.Constants;
import io.github.vipcxj.jasync.core.javac.utils.SymbolHelpers;
import io.github.vipcxj.jasync.runtime.helpers.*;
import io.github.vipcxj.jasync.spec.JAsync;
import io.github.vipcxj.jasync.spec.JPromise;
import io.github.vipcxj.jasync.spec.catcher.Catcher;
import io.github.vipcxj.jasync.spec.catcher.Catchers;
import io.github.vipcxj.jasync.spec.functional.*;
import io.github.vipcxj.jasync.spec.switchexpr.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JAsyncSymbols {
    protected static final Context.Key<JAsyncSymbols> symbolsKey = new Context.Key<>();

    private final TreeMaker maker;
    private final Types types;
    private final Names names;
    private final Symbol.MethodSymbol symPromiseThenSupplierArg;
    private final Symbol.MethodSymbol symPromiseThenFuncArg;
    private final Symbol.MethodSymbol symPromiseThenVoidSupplierArg;
    private final Symbol.MethodSymbol symPromiseThenVoidFuncArg;
    private final Symbol.MethodSymbol symPromiseDoCatch;
    private final Symbol.MethodSymbol symPromiseDoFinally;
    private final Symbol.MethodSymbol symPromiseCatchReturn;
    private final Symbol.ClassSymbol symJAsync;
    private final Symbol.MethodSymbol symJAsyncJustValue;
    private final Symbol.MethodSymbol symJAsyncDeferVoid;
    private final Symbol.MethodSymbol symJAsyncDoIf;
    private final Symbol.MethodSymbol symJAsyncDoFor;
    private final Symbol.MethodSymbol symJAsyncDoPromiseFor;
    private final Symbol.MethodSymbol symJAsyncDoSwitch;
    private final Symbol.MethodSymbol symJAsyncDoWhile;
    private final Symbol.MethodSymbol symJAsyncDoPromiseWhile;
    private final Symbol.MethodSymbol symJAsyncDoDoWhile;
    private final Symbol.MethodSymbol symJAsyncDoDoPromiseWhile;
    private final Symbol.MethodSymbol symJAsyncDoForEachByte;
    private final Symbol.MethodSymbol symJAsyncDoForEachChar;
    private final Symbol.MethodSymbol symJAsyncDoForEachShort;
    private final Symbol.MethodSymbol symJAsyncDoForEachInt;
    private final Symbol.MethodSymbol symJAsyncDoForEachLong;
    private final Symbol.MethodSymbol symJAsyncDoForEachFloat;
    private final Symbol.MethodSymbol symJAsyncDoForEachDouble;
    private final Symbol.MethodSymbol symJAsyncDoForEachBoolean;
    private final Symbol.MethodSymbol symJAsyncDoForEachObject;
    private final Symbol.MethodSymbol symJAsyncDoBreak;
    private final Symbol.MethodSymbol symJAsyncDoContinue;
    private final Symbol.MethodSymbol symJAsyncDoReturn;
    private final Symbol.ClassSymbol symDefaultCase;
    private final Symbol.MethodSymbol symDefaultCaseOf;
    private final Symbol.ClassSymbol symIntCase;
    private final Symbol.MethodSymbol symIntCaseOf;
    private final Symbol.ClassSymbol symStringCase;
    private final Symbol.MethodSymbol symStringCaseOf;
    private final Symbol.ClassSymbol symEnumCase;
    private final Symbol.MethodSymbol symEnumCaseOf;
    private final Symbol.ClassSymbol symCases;
    private final Symbol.MethodSymbol symCasesOf;
    private final Symbol.ClassSymbol symCatcher;
    private final Symbol.MethodSymbol symCatcherOf;
    private final Symbol.ClassSymbol symCatchers;
    private final Symbol.MethodSymbol symCatchersOf;
    private final Symbol.MethodSymbol symBooleanRefGet;
    private final Symbol.MethodSymbol symByteRefGet;
    private final Symbol.MethodSymbol symCharRefGet;
    private final Symbol.MethodSymbol symDoubleRefGet;
    private final Symbol.MethodSymbol symFloatRefGet;
    private final Symbol.MethodSymbol symIntRefGet;
    private final Symbol.MethodSymbol symLongRefGet;
    private final Symbol.MethodSymbol symShortRefGet;
    private final Symbol.MethodSymbol symObjectRefGet;
    private final Symbol.ClassSymbol[] refOwnerSymbols;
    private final Class<?>[] refOwnerBaseTypes;
    private final Map<Long, Symbol.MethodSymbol> refMethodSymbols;

    protected JAsyncSymbols(Context context) {
        context.put(symbolsKey, this);
        maker = TreeMaker.instance(context);
        types = Types.instance(context);
        JavacElements elements = JavacElements.instance(context);
        names = Names.instance(context);
        Symbol.ClassSymbol symPromise = elements.getTypeElement(JPromise.class.getCanonicalName());
        symPromiseThenSupplierArg = SymbolHelpers.INSTANCE.getMethodMember(
                types, symPromise,
                names.fromString(Constants.THEN),
                false,
                PromiseSupplier.class
        );
        symPromiseThenFuncArg = SymbolHelpers.INSTANCE.getMethodMember(
                types, symPromise,
                names.fromString(Constants.THEN),
                false,
                PromiseFunction.class
        );
        symPromiseThenVoidSupplierArg = SymbolHelpers.INSTANCE.getMethodMember(
                types, symPromise,
                names.fromString(Constants.THEN_VOID),
                false,
                VoidPromiseSupplier.class
        );
        symPromiseThenVoidFuncArg = SymbolHelpers.INSTANCE.getMethodMember(
                types, symPromise,
                names.fromString(Constants.THEN_VOID),
                false,
                VoidPromiseFunction.class
        );
        symPromiseDoCatch = SymbolHelpers.INSTANCE.getMethodMember(
                types, symPromise,
                names.fromString(Constants.DO_CATCH),
                false,
                List.class
        );
        symPromiseDoFinally = SymbolHelpers.INSTANCE.getMethodMember(
                types, symPromise,
                names.fromString(Constants.DO_FINALLY),
                false,
                VoidPromiseSupplier.class
        );
        symPromiseCatchReturn = SymbolHelpers.INSTANCE.getMethodMember(
                types, symPromise,
                names.fromString(Constants.CATCH_RETURN),
                false
        );
        symJAsync = elements.getTypeElement(JAsync.class.getCanonicalName());
        symJAsyncJustValue = SymbolHelpers.INSTANCE.getMethodMember(
                types, symJAsync,
                names.fromString(Constants.JUST),
                true,
                Object.class
        );
        symJAsyncDeferVoid = SymbolHelpers.INSTANCE.getMethodMember(
                types, symJAsync,
                names.fromString(Constants.DEFER_VOID),
                true,
                VoidPromiseSupplier.class, String.class
        );
        symJAsyncDoIf = SymbolHelpers.INSTANCE.getMethodMember(
                types, symJAsync,
                names.fromString(Constants.DO_IF),
                true,
                boolean.class, VoidPromiseSupplier.class, VoidPromiseSupplier.class
        );
        symJAsyncDoFor = SymbolHelpers.INSTANCE.getMethodMember(
                types, symJAsync,
                names.fromString(Constants.DO_FOR),
                true,
                VoidPromiseSupplier.class, BooleanSupplier.class, VoidPromiseSupplier.class, VoidPromiseSupplier.class, String.class
        );
        symJAsyncDoPromiseFor = SymbolHelpers.INSTANCE.getMethodMember(
                types, symJAsync,
                names.fromString(Constants.DO_FOR),
                true,
                VoidPromiseSupplier.class, PromiseSupplier.class, VoidPromiseSupplier.class, VoidPromiseSupplier.class, String.class
        );
        symJAsyncDoSwitch = SymbolHelpers.INSTANCE.getMethodMember(
                types, symJAsync,
                names.fromString(Constants.DO_SWITCH),
                true,
                Object.class, List.class, String.class
        );
        symJAsyncDoWhile = SymbolHelpers.INSTANCE.getMethodMember(
                types, symJAsync,
                names.fromString(Constants.DO_WHILE),
                true,
                BooleanSupplier.class, VoidPromiseSupplier.class, String.class
        );
        symJAsyncDoPromiseWhile = SymbolHelpers.INSTANCE.getMethodMember(
                types, symJAsync,
                names.fromString(Constants.DO_WHILE),
                true,
                PromiseSupplier.class, VoidPromiseSupplier.class, String.class
        );
        symJAsyncDoDoWhile = SymbolHelpers.INSTANCE.getMethodMember(
                types, symJAsync,
                names.fromString(Constants.DO_DO_WHILE),
                true,
                BooleanSupplier.class, VoidPromiseSupplier.class, String.class
        );
        symJAsyncDoDoPromiseWhile = SymbolHelpers.INSTANCE.getMethodMember(
                types, symJAsync,
                names.fromString(Constants.DO_DO_WHILE),
                true,
                PromiseSupplier.class, VoidPromiseSupplier.class, String.class
        );
        symJAsyncDoForEachByte = SymbolHelpers.INSTANCE.getMethodMember(
                types, symJAsync,
                names.fromString(Constants.DO_FOR_EACH_BYTE),
                true,
                Object.class, ByteVoidPromiseFunction.class, String.class
        );
        symJAsyncDoForEachChar = SymbolHelpers.INSTANCE.getMethodMember(
                types, symJAsync,
                names.fromString(Constants.DO_FOR_EACH_CHAR),
                true,
                Object.class, CharVoidPromiseFunction.class, String.class
        );
        symJAsyncDoForEachShort = SymbolHelpers.INSTANCE.getMethodMember(
                types, symJAsync,
                names.fromString(Constants.DO_FOR_EACH_SHORT),
                true,
                Object.class, ShortVoidPromiseFunction.class, String.class
        );
        symJAsyncDoForEachInt = SymbolHelpers.INSTANCE.getMethodMember(
                types, symJAsync,
                names.fromString(Constants.DO_FOR_EACH_INT),
                true,
                Object.class, IntVoidPromiseFunction.class, String.class
        );
        symJAsyncDoForEachLong = SymbolHelpers.INSTANCE.getMethodMember(
                types, symJAsync,
                names.fromString(Constants.DO_FOR_EACH_LONG),
                true,
                Object.class, LongVoidPromiseFunction.class, String.class
        );
        symJAsyncDoForEachFloat = SymbolHelpers.INSTANCE.getMethodMember(
                types, symJAsync,
                names.fromString(Constants.DO_FOR_EACH_FLOAT),
                true,
                Object.class, FloatVoidPromiseFunction.class, String.class
        );
        symJAsyncDoForEachDouble = SymbolHelpers.INSTANCE.getMethodMember(
                types, symJAsync,
                names.fromString(Constants.DO_FOR_EACH_DOUBLE),
                true,
                Object.class, DoubleVoidPromiseFunction.class, String.class
        );
        symJAsyncDoForEachBoolean = SymbolHelpers.INSTANCE.getMethodMember(
                types, symJAsync,
                names.fromString(Constants.DO_FOR_EACH_BOOLEAN),
                true,
                Object.class, BooleanVoidPromiseFunction.class, String.class
        );
        symJAsyncDoForEachObject = SymbolHelpers.INSTANCE.getMethodMember(
                types, symJAsync,
                names.fromString(Constants.DO_FOR_EACH_OBJECT),
                true,
                Object.class, VoidPromiseFunction.class, String.class
        );
        symJAsyncDoBreak = SymbolHelpers.INSTANCE.getMethodMember(
                types, symJAsync,
                names.fromString(Constants.DO_BREAK),
                true,
                String.class
        );
        symJAsyncDoContinue = SymbolHelpers.INSTANCE.getMethodMember(
                types, symJAsync,
                names.fromString(Constants.DO_CONTINUE),
                true,
                String.class
        );
        symJAsyncDoReturn = SymbolHelpers.INSTANCE.getMethodMember(
                types, symJAsync,
                names.fromString(Constants.DO_RETURN),
                true,
                JPromise.class
        );
        symDefaultCase = elements.getTypeElement(DefaultCase.class.getCanonicalName());
        symDefaultCaseOf = SymbolHelpers.INSTANCE.getMethodMember(
                types, symDefaultCase,
                names.fromString(Constants.OF),
                true,
                VoidPromiseSupplier.class
        );
        symIntCase = elements.getTypeElement(IntCase.class.getCanonicalName());
        symIntCaseOf = SymbolHelpers.INSTANCE.getMethodMember(
                types, symIntCase,
                names.fromString(Constants.OF),
                true,
                int.class, VoidPromiseSupplier.class
        );
        symStringCase = elements.getTypeElement(StringCase.class.getCanonicalName());
        symStringCaseOf = SymbolHelpers.INSTANCE.getMethodMember(
                types, symStringCase,
                names.fromString(Constants.OF),
                true,
                String.class, VoidPromiseSupplier.class
        );
        symEnumCase = elements.getTypeElement(EnumCase.class.getCanonicalName());
        symEnumCaseOf = SymbolHelpers.INSTANCE.getMethodMember(
                types, symEnumCase,
                names.fromString(Constants.OF),
                true,
                Enum.class, VoidPromiseSupplier.class
        );
        symCases = elements.getTypeElement(Cases.class.getCanonicalName());
        symCasesOf = SymbolHelpers.INSTANCE.getMethodMember(
                types, symCases,
                names.fromString(Constants.OF),
                true,
                ICase[].class
        );
        symCatcher = elements.getTypeElement(Catcher.class.getCanonicalName());
        symCatcherOf = SymbolHelpers.INSTANCE.getMethodMember(
                types, symCatcher,
                names.fromString(Constants.OF),
                true,
                Class.class, PromiseFunction.class
        );
        symCatchers = elements.getTypeElement(Catchers.class.getCanonicalName());
        symCatchersOf = SymbolHelpers.INSTANCE.getMethodMember(
                types, symCatchers,
                names.fromString(Constants.OF),
                true,
                Catcher[].class
        );
        Name refGet = names.fromString(Constants.REFERENCE_GET);
        Symbol.ClassSymbol symBooleanRef = elements.getTypeElement(BooleanReference.class.getCanonicalName());
        symBooleanRefGet = SymbolHelpers.INSTANCE.getMethodMember(types, symBooleanRef, refGet, false);
        Symbol.ClassSymbol symByteRef = elements.getTypeElement(ByteReference.class.getCanonicalName());
        symByteRefGet = SymbolHelpers.INSTANCE.getMethodMember(types, symByteRef, refGet, false);
        Symbol.ClassSymbol symCharRef = elements.getTypeElement(CharReference.class.getCanonicalName());
        symCharRefGet = SymbolHelpers.INSTANCE.getMethodMember(types, symCharRef, refGet, false);
        Symbol.ClassSymbol symDoubleRef = elements.getTypeElement(DoubleReference.class.getCanonicalName());
        symDoubleRefGet = SymbolHelpers.INSTANCE.getMethodMember(types, symDoubleRef, refGet, false);
        Symbol.ClassSymbol symFloatRef = elements.getTypeElement(FloatReference.class.getCanonicalName());
        symFloatRefGet = SymbolHelpers.INSTANCE.getMethodMember(types, symFloatRef, refGet, false);
        Symbol.ClassSymbol symIntRef = elements.getTypeElement(IntReference.class.getCanonicalName());
        symIntRefGet = SymbolHelpers.INSTANCE.getMethodMember(types, symIntRef, refGet, false);
        Symbol.ClassSymbol symLongRef = elements.getTypeElement(LongReference.class.getCanonicalName());
        symLongRefGet = SymbolHelpers.INSTANCE.getMethodMember(types, symLongRef, refGet, false);
        Symbol.ClassSymbol symShortRef = elements.getTypeElement(ShortReference.class.getCanonicalName());
        symShortRefGet = SymbolHelpers.INSTANCE.getMethodMember(types, symShortRef, refGet, false);
        Symbol.ClassSymbol symObjectRef = elements.getTypeElement(ObjectReference.class.getCanonicalName());
        symObjectRefGet = SymbolHelpers.INSTANCE.getMethodMember(types, symObjectRef, refGet, false);
        refOwnerSymbols = new Symbol.ClassSymbol[] {
                symBooleanRef,
                symByteRef,
                symCharRef,
                symDoubleRef,
                symFloatRef,
                symIntRef,
                symLongRef,
                symObjectRef,
                symShortRef
        };
        refOwnerBaseTypes = new Class[] {
                boolean.class,
                byte.class,
                char.class,
                double.class,
                float.class,
                int.class,
                long.class,
                Object.class,
                short.class
        };
        refMethodSymbols = new HashMap<>();
    }

    public static JAsyncSymbols instance(Context context) {
        JAsyncSymbols instance = context.get(symbolsKey);
        if (instance == null)
            instance = new JAsyncSymbols(context);
        return instance;
    }

    public JCTree.JCExpression makeJust() {
        return makeJust(maker.Literal(TypeTag.BOT, null));
    }

    public JCTree.JCExpression makeJust(JCTree.JCExpression value) {
        int prePos = maker.pos;
        try {
            maker.at(value);
            return maker.Apply(
                    com.sun.tools.javac.util.List.nil(),
                    maker.Select(maker.QualIdent(symJAsync), symJAsyncJustValue),
                    com.sun.tools.javac.util.List.of(value)
            );
        } finally {
            maker.pos = prePos;
        }
    }

    public JCTree.JCExpression makeCatchReturn(JCTree.JCExpression expression) {
        int prePos = maker.pos;
        try {
            maker.at(expression);
            return maker.Apply(
                    com.sun.tools.javac.util.List.nil(),
                    maker.Select(expression, symPromiseCatchReturn),
                    com.sun.tools.javac.util.List.nil()
            );
        } finally {
            maker.pos = prePos;
        }
    }

    public JCTree.JCExpression makeJAsyncDeferVoid() {
        return maker.Select(maker.QualIdent(symJAsync), symJAsyncDeferVoid);
    }

    public JCTree.JCExpression makeJAsyncDoIf() {
        return maker.Select(maker.QualIdent(symJAsync), symJAsyncDoIf);
    }

    public JCTree.JCExpression makeJAsyncDoFor() {
        return maker.Select(maker.QualIdent(symJAsync), symJAsyncDoFor);
    }

    public JCTree.JCExpression makeJAsyncDoPromiseFor() {
        return maker.Select(maker.QualIdent(symJAsync), symJAsyncDoPromiseFor);
    }

    public JCTree.JCExpression makeJAsyncDoSwitch() {
        return maker.Select(maker.QualIdent(symJAsync), symJAsyncDoSwitch);
    }

    public JCTree.JCExpression makeJAsyncDoWhile() {
        return maker.Select(maker.QualIdent(symJAsync), symJAsyncDoWhile);
    }

    public JCTree.JCExpression makeJAsyncDoPromiseWhile() {
        return maker.Select(maker.QualIdent(symJAsync), symJAsyncDoPromiseWhile);
    }

    public JCTree.JCExpression makeJAsyncDoDoWhile() {
        return maker.Select(maker.QualIdent(symJAsync), symJAsyncDoDoWhile);
    }

    public JCTree.JCExpression makeJAsyncDoDoPromiseWhile() {
        return maker.Select(maker.QualIdent(symJAsync), symJAsyncDoDoPromiseWhile);
    }

    public JCTree.JCExpression makeJAsyncDoForEachByte() {
        return maker.Select(maker.QualIdent(symJAsync), symJAsyncDoForEachByte);
    }

    public JCTree.JCExpression makeJAsyncDoForEachChar() {
        return maker.Select(maker.QualIdent(symJAsync), symJAsyncDoForEachChar);
    }

    public JCTree.JCExpression makeJAsyncDoForEachShort() {
        return maker.Select(maker.QualIdent(symJAsync), symJAsyncDoForEachShort);
    }

    public JCTree.JCExpression makeJAsyncDoForEachInt() {
        return maker.Select(maker.QualIdent(symJAsync), symJAsyncDoForEachInt);
    }

    public JCTree.JCExpression makeJAsyncDoForEachLong() {
        return maker.Select(maker.QualIdent(symJAsync), symJAsyncDoForEachLong);
    }

    public JCTree.JCExpression makeJAsyncDoForEachFloat() {
        return maker.Select(maker.QualIdent(symJAsync), symJAsyncDoForEachFloat);
    }

    public JCTree.JCExpression makeJAsyncDoForEachDouble() {
        return maker.Select(maker.QualIdent(symJAsync), symJAsyncDoForEachDouble);
    }

    public JCTree.JCExpression makeJAsyncDoForEachBoolean() {
        return maker.Select(maker.QualIdent(symJAsync), symJAsyncDoForEachBoolean);
    }

    public JCTree.JCExpression makeJAsyncDoForEachObject() {
        return maker.Select(maker.QualIdent(symJAsync), symJAsyncDoForEachObject);
    }

    public JCTree.JCExpression makeDoReturn(JCTree.JCReturn tree, JCTree.JCExpression expr) {
        int prePos = maker.pos;
        try {
            maker.at(tree);
            return maker.Apply(
                    com.sun.tools.javac.util.List.nil(),
                    maker.Select(maker.QualIdent(symJAsync), symJAsyncDoReturn),
                    com.sun.tools.javac.util.List.of(
                            expr != null ? expr : maker.Literal(TypeTag.BOT, null)
                    )
            );
        } finally {
            maker.pos = prePos;
        }
    }

    private JCTree.JCExpression makeBreakLike(JCTree tree, Name label, Symbol.MethodSymbol symbol) {
        int prePos = maker.pos;
        try {
            maker.at(tree);
            boolean hasLabel = label != null && !label.isEmpty();
            return maker.Apply(
                    com.sun.tools.javac.util.List.nil(),
                    maker.Select(maker.QualIdent(symJAsync), symbol),
                    com.sun.tools.javac.util.List.of(
                            hasLabel ? maker.Literal(label.toString()) : maker.Literal(TypeTag.BOT, null)
                    )
            );
        } finally {
            maker.pos = prePos;
        }
    }

    public JCTree.JCExpression makeDoBreak(JCTree.JCBreak tree) {
        return makeBreakLike(tree, tree.label, symJAsyncDoBreak);
    }

    public JCTree.JCExpression makeDoContinue(JCTree.JCContinue tree) {
        return makeBreakLike(tree, tree.label, symJAsyncDoContinue);
    }

    public JCTree.JCExpression makeDefaultCaseOf() {
        return maker.Select(maker.QualIdent(symDefaultCase), symDefaultCaseOf);
    }

    public JCTree.JCExpression makeIntCaseOf() {
        return maker.Select(maker.QualIdent(symIntCase), symIntCaseOf);
    }

    public JCTree.JCExpression makeStringCaseOf() {
        return maker.Select(maker.QualIdent(symStringCase), symStringCaseOf);
    }

    public JCTree.JCExpression makeEnumCaseOf() {
        return maker.Select(maker.QualIdent(symEnumCase), symEnumCaseOf);
    }

    public JCTree.JCExpression makeCasesOf() {
        return maker.Select(maker.QualIdent(symCases), symCasesOf);
    }

    public JCTree.JCExpression makeCatcherOf() {
        return maker.Select(maker.QualIdent(symCatcher), symCatcherOf);
    }

    public JCTree.JCExpression makeCatchersOf() {
        return maker.Select(maker.QualIdent(symCatchers), symCatchersOf);
    }

    public JCTree.JCExpression makeRefGet(Symbol.VarSymbol symRef) {
        String qName = symRef.type.asElement().getQualifiedName().toString();
        Symbol.MethodSymbol methodSymbol;
        if (BooleanReference.class.getCanonicalName().equals(qName)) {
            methodSymbol = symBooleanRefGet;
        } else if (ByteReference.class.getCanonicalName().equals(qName)) {
            methodSymbol = symByteRefGet;
        } else if (CharReference.class.getCanonicalName().equals(qName)) {
            methodSymbol = symCharRefGet;
        } else if (DoubleReference.class.getCanonicalName().equals(qName)) {
            methodSymbol = symDoubleRefGet;
        } else if (FloatReference.class.getCanonicalName().equals(qName)) {
            methodSymbol = symFloatRefGet;
        } else if (IntReference.class.getCanonicalName().equals(qName)) {
            methodSymbol = symIntRefGet;
        } else if (LongReference.class.getCanonicalName().equals(qName)) {
            methodSymbol = symLongRefGet;
        } else if (ObjectReference.class.getCanonicalName().equals(qName)) {
            methodSymbol = symObjectRefGet;
        } else if (ShortReference.class.getCanonicalName().equals(qName)) {
            methodSymbol = symShortRefGet;
        } else {
            throw new IllegalArgumentException("Not a reference type symbol: " + symRef.type.toString() + " " + symRef.getSimpleName());
        }
        return maker.Apply(
                com.sun.tools.javac.util.List.nil(),
                maker.Select(maker.Ident(symRef), methodSymbol),
                com.sun.tools.javac.util.List.nil()
        );
    }

    private int getRefOwnerIndex(Symbol.VarSymbol symRef) {
        String qName = symRef.type.asElement().getQualifiedName().toString();
        if (BooleanReference.class.getCanonicalName().equals(qName)) {
            return 0;
        } else if (ByteReference.class.getCanonicalName().equals(qName)) {
            return 1;
        } else if (CharReference.class.getCanonicalName().equals(qName)) {
            return 2;
        } else if (DoubleReference.class.getCanonicalName().equals(qName)) {
            return 3;
        } else if (FloatReference.class.getCanonicalName().equals(qName)) {
            return 4;
        } else if (IntReference.class.getCanonicalName().equals(qName)) {
            return 5;
        } else if (LongReference.class.getCanonicalName().equals(qName)) {
            return 6;
        } else if (ObjectReference.class.getCanonicalName().equals(qName)) {
            return 7;
        } else if (ShortReference.class.getCanonicalName().equals(qName)) {
            return 8;
        } else {
            throw new IllegalArgumentException("Not a reference type symbol: " + symRef.type.toString() + " " + symRef.getSimpleName());
        }
    }

    private boolean isIntegralPrimitive(Type type) {
        if (!type.isPrimitive()) {
            return false;
        }
        switch (type.getTag()) {
            case CHAR:
            case BYTE:
            case SHORT:
            case INT:
            case LONG:
                return true;
            default:
                return false;
        }
    }

    private boolean isNumericPrimitive(Type type) {
        if (!type.isPrimitive()) {
            return false;
        }
        switch (type.getTag()) {
            case BOOLEAN:
            case VOID:
                return false;
            default:
                return true;
        }
    }

    // 1: integer, 2: decimal, 0: other
    private int getRefExprType(Type type) {
        Type unboxedType = types.unboxedTypeOrType(type);
        if (!unboxedType.isPrimitive() || unboxedType.hasTag(TypeTag.VOID)) {
            return 0;
        }
        if (isIntegralPrimitive(type)) {
            return 1;
        } else if (isNumericPrimitive(type)) {
            return 2;
        } else {
            return 0;
        }
    }

    /**
     * 0: no arg.
     * 1: one arg, accept only integer.
     * 2: one arg, accept integer or decimal.
     * 3: one arg, accept self type.
     * 4: one arg, accept integer or decimal or string.
     * -1: others.
     * @param tag tag
     * @return type
     */
    private int getRefTagType(JCTree.Tag tag) {
        switch (tag) {
            case ASSIGN:
                return 3;
            case PLUS_ASG:
                return 4;
            case PREINC:
            case PREDEC:
            case POSTINC:
            case POSTDEC:
                return 0;
            case MINUS_ASG:
            case DIV_ASG:
            case MUL_ASG:
            case MOD_ASG:
                return 2;
            case SL_ASG:
            case SR_ASG:
            case USR_ASG:
            case BITAND_ASG:
            case BITOR_ASG:
            case BITXOR_ASG:
                return 1;
            default:
                return -1;
        }
    }

    private RefMethod getRefMethod(JCTree.Tag tag) {
        switch (tag) {
            case ASSIGN:
                return RefMethod.REFERENCE_ASSIGN;
            case PREINC:
                return RefMethod.REFERENCE_PRE_INC;
            case PREDEC:
                return RefMethod.REFERENCE_PRE_DEC;
            case POSTINC:
                return RefMethod.REFERENCE_POST_INC;
            case POSTDEC:
                return RefMethod.REFERENCE_POST_DEC;
            case PLUS_ASG:
                return RefMethod.REFERENCE_PLUS_ASSIGN;
            case MINUS_ASG:
                return RefMethod.REFERENCE_MINUS_ASSIGN;
            case DIV_ASG:
                return RefMethod.REFERENCE_DIVIDE_ASSIGN;
            case MUL_ASG:
                return RefMethod.REFERENCE_MULTIPLY_ASSIGN;
            case MOD_ASG:
                return RefMethod.REFERENCE_MOD_ASSIGN;
            case SL_ASG:
                return RefMethod.REFERENCE_LEFT_SHIFT_ASSIGN;
            case SR_ASG:
                return RefMethod.REFERENCE_RIGHT_SHIFT_ASSIGN;
            case USR_ASG:
                return RefMethod.REFERENCE_UNSIGNED_RIGHT_SHIFT_ASSIGN;
            case BITAND_ASG:
                return RefMethod.REFERENCE_LOGIC_AND_ASSIGN;
            case BITOR_ASG:
                return RefMethod.REFERENCE_LOGIC_OR_ASSIGN;
            case BITXOR_ASG:
                return RefMethod.REFERENCE_LOGIC_XOR_ASSIGN;
            default:
                throw new IllegalArgumentException("Invalid ref method tag: " + tag + ".");
        }
    }

    public Symbol.MethodSymbol getRefAssignSymbol(Symbol.VarSymbol symRef, JCTree.Tag tag, JCTree.JCExpression expr) {
        int refOwnerIndex = getRefOwnerIndex(symRef);
        Symbol.ClassSymbol refOwnerSymbol = refOwnerSymbols[refOwnerIndex];
        RefMethod refMethod = getRefMethod(tag);
        Name refMethodName = names.fromString(refMethod.name);
        long key = refOwnerIndex | ((long) refMethod.ordinal() << 4);
        Symbol.MethodSymbol methodSymbol = null;
        int refTagType = getRefTagType(tag);
        if (refTagType == 0) {
            methodSymbol = refMethodSymbols.get(key);
            if (methodSymbol == null) {
                methodSymbol = SymbolHelpers.INSTANCE.getMethodMember(types, refOwnerSymbol, refMethodName, false);
                refMethodSymbols.put(key, methodSymbol);
            }
        } else if (refTagType == 1) {
            methodSymbol = refMethodSymbols.get(key);
            if (methodSymbol == null) {
                methodSymbol = SymbolHelpers.INSTANCE.getMethodMember(types, refOwnerSymbol, refMethodName, false, long.class);
                refMethodSymbols.put(key, methodSymbol);
            }
        } else if (refTagType == 2 || refTagType == 4) {
            int refExprType = getRefExprType(expr.type);
            if (refExprType == 2) {
                key |= 1 << 8;
            } else if (refExprType == 0) {
                key |= 2 << 8;
            }
            methodSymbol = refMethodSymbols.get(key);
            if (methodSymbol == null) {
                if (refExprType == 1) {
                    methodSymbol = SymbolHelpers.INSTANCE.getMethodMember(types, refOwnerSymbol, refMethodName, false, long.class);
                } else if (refExprType == 2) {
                    methodSymbol = SymbolHelpers.INSTANCE.getMethodMember(types, refOwnerSymbol, refMethodName, false, double.class);
                } else if (refExprType == 0) {
                    methodSymbol = SymbolHelpers.INSTANCE.getMethodMember(types, refOwnerSymbol, refMethodName, false, Object.class);
                } else {
                    throw new IllegalArgumentException("Invalid expr type: " + expr.type + ".");
                }
                refMethodSymbols.put(key, methodSymbol);
            }
        } else if (refTagType == 3) {
            key |= 2 << 8;
            methodSymbol = refMethodSymbols.get(key);
            if (methodSymbol == null) {
                Class<?> refOwnerBaseType = refOwnerBaseTypes[refOwnerIndex];
                methodSymbol = SymbolHelpers.INSTANCE.getMethodMember(types, refOwnerSymbol, refMethodName, false, refOwnerBaseType);
                refMethodSymbols.put(key, methodSymbol);
            }
        }
        if (methodSymbol == null) {
            throw new IllegalArgumentException("Unable to find the method symbol for " + tag + " of " + refOwnerSymbol + ".");
        }
        return methodSymbol;
    }

    public JCTree.JCMethodInvocation makeRefAssign(Symbol.VarSymbol symRef, JCTree.Tag tag, JCTree.JCExpression expr) {
        Symbol.MethodSymbol methodSymbol = getRefAssignSymbol(symRef, tag, expr);
        return maker.Apply(
                com.sun.tools.javac.util.List.nil(),
                maker.Select(maker.Ident(symRef), methodSymbol),
                expr != null ? com.sun.tools.javac.util.List.of(expr) : com.sun.tools.javac.util.List.nil()
        );
    }

    public JCTree.JCExpression makePromiseThenFuncArg(JCTree.JCExpression expr) {
        return expr != null ? maker.Select(expr, symPromiseThenFuncArg) : maker.Ident(symPromiseThenFuncArg);
    }

    public JCTree.JCExpression makePromiseThenSupplierArg(JCTree.JCExpression expr) {
        return expr != null ? maker.Select(expr, symPromiseThenSupplierArg) : maker.Ident(symPromiseThenSupplierArg);
    }

    public JCTree.JCExpression makePromiseThenVoidFuncArg(JCTree.JCExpression expr) {
        return expr != null ? maker.Select(expr, symPromiseThenVoidFuncArg) : maker.Ident(symPromiseThenVoidFuncArg);
    }

    public JCTree.JCExpression makePromiseThenVoidSupplierArg(JCTree.JCExpression expr) {
        return expr != null ? maker.Select(expr, symPromiseThenVoidSupplierArg) : maker.Ident(symPromiseThenVoidSupplierArg);
    }

    public JCTree.JCExpression makePromiseDoCatch(JCTree.JCExpression expr) {
        return expr != null ? maker.Select(expr, symPromiseDoCatch) : maker.Ident(symPromiseDoCatch);
    }

    public JCTree.JCExpression makePromiseDoFinally(JCTree.JCExpression expr) {
        return expr != null ? maker.Select(expr, symPromiseDoFinally) : maker.Ident(symPromiseDoFinally);
    }

    enum RefMethod {
        REFERENCE_ASSIGN(Constants.REFERENCE_ASSIGN),
        REFERENCE_PRE_INC(Constants.REFERENCE_PRE_INC),
        REFERENCE_PRE_DEC(Constants.REFERENCE_PRE_DEC),
        REFERENCE_POST_INC(Constants.REFERENCE_POST_INC),
        REFERENCE_POST_DEC(Constants.REFERENCE_POST_DEC),
        REFERENCE_PLUS_ASSIGN(Constants.REFERENCE_PLUS_ASSIGN),
        REFERENCE_MINUS_ASSIGN(Constants.REFERENCE_MINUS_ASSIGN),
        REFERENCE_DIVIDE_ASSIGN(Constants.REFERENCE_DIVIDE_ASSIGN),
        REFERENCE_MULTIPLY_ASSIGN(Constants.REFERENCE_MULTIPLY_ASSIGN),
        REFERENCE_MOD_ASSIGN(Constants.REFERENCE_MOD_ASSIGN),
        REFERENCE_LEFT_SHIFT_ASSIGN(Constants.REFERENCE_LEFT_SHIFT_ASSIGN),
        REFERENCE_RIGHT_SHIFT_ASSIGN(Constants.REFERENCE_RIGHT_SHIFT_ASSIGN),
        REFERENCE_UNSIGNED_RIGHT_SHIFT_ASSIGN(Constants.REFERENCE_UNSIGNED_RIGHT_SHIFT_ASSIGN),
        REFERENCE_LOGIC_AND_ASSIGN(Constants.REFERENCE_LOGIC_AND_ASSIGN),
        REFERENCE_LOGIC_OR_ASSIGN(Constants.REFERENCE_LOGIC_OR_ASSIGN),
        REFERENCE_LOGIC_XOR_ASSIGN(Constants.REFERENCE_LOGIC_XOR_ASSIGN);
        private final String name;
        RefMethod(String name) {
            this.name = name;
        }
    }
}
