package io.github.vipcxj.jasync.core.javac.patch;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.util.Name;
import io.github.vipcxj.jasync.core.javac.JavacUtils;
import io.github.vipcxj.jasync.core.javac.patch.spi.SymbolHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Predicate;

public enum SymbolHelpers {
    INSTANCE;
    private SymbolHelper helper;
    SymbolHelpers() {
        int mainVersion = JavacUtils.getJavaMainVersion();
        List<SymbolHelper> helpers = new ArrayList<>();
        for (SymbolHelper symbolHelper : ServiceLoader.load(SymbolHelper.class, SymbolHelpers.class.getClassLoader())) {
            helpers.add(symbolHelper);
        }
        helpers.sort(Comparator.comparingInt(SymbolHelper::supportVersion));
        for (SymbolHelper symbolHelper : helpers) {
            if (symbolHelper.supportVersion() <= mainVersion) {
                helper = symbolHelper;
            } else {
                break;
            }
        }
        if (helper == null) {
            throw new IllegalStateException("Unable to find a impl of SymbolHelper, current java main version is " + mainVersion + ".");
        }
    }

    public Symbol getSymbol(Symbol.TypeSymbol typeSymbol, Name name, Predicate<Symbol> filter) {
        return helper.getSymbol(typeSymbol, name, filter);
    }

    private boolean equalType(Types types, Type type, Class<?> clazz) {
        if (type.isPrimitive()) {
            if (!clazz.isPrimitive()) {
                return false;
            }
            if (type.hasTag(TypeTag.BYTE)) {
                return clazz == boolean.class;
            } else if (type.hasTag(TypeTag.CHAR)) {
                return clazz == char.class;
            } else if (type.hasTag(TypeTag.SHORT)) {
                return clazz == short.class;
            } else if (type.hasTag(TypeTag.BOOLEAN)) {
                return clazz == boolean.class;
            } else if (type.hasTag(TypeTag.DOUBLE)) {
                return clazz == double.class;
            } else if (type.hasTag(TypeTag.FLOAT)) {
                return clazz == float.class;
            } else if (type.hasTag(TypeTag.INT)) {
                return clazz == int.class;
            } else if (type.hasTag(TypeTag.LONG)) {
                return clazz == long.class;
            } else {
                throw new IllegalArgumentException("Unknown primitive type: " + type + ".");
            }
        } else if (types.isArray(type)) {
            if (!clazz.isArray()) {
                return false;
            }
            return equalType(types, types.elemtype(type), clazz.getComponentType());
        } else if (type instanceof Type.TypeVar) {
            return equalType(types, type.getUpperBound(), clazz);
        } else {
            return type.asElement().getQualifiedName().toString().equals(clazz.getCanonicalName());
        }
    }

    public Symbol.MethodSymbol getMethodMember(Types types, Symbol.TypeSymbol typeSymbol, Name name, boolean isStatic, Class<?>... args) {
        return (Symbol.MethodSymbol) helper.getSymbol(typeSymbol, name, symbol -> {
            if (symbol instanceof Symbol.MethodSymbol) {
                Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) symbol;
                if (isStatic && !methodSymbol.isStatic()) {
                    return false;
                }
                if (!isStatic && methodSymbol.isStatic()) {
                    return false;
                }
                if (args.length != methodSymbol.params.size()) {
                    return false;
                }
                int i = 0;
                for (Symbol.VarSymbol param : methodSymbol.params) {
                    Class<?> argType = args[i++];
                    if (!equalType(types, param.type, argType)) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        });
    }
}
