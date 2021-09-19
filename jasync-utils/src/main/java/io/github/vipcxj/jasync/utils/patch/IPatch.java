package io.github.vipcxj.jasync.utils.patch;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Name;

import java.util.function.Predicate;

public interface IPatch {

    Symbol getSymbol(Symbol.TypeSymbol classSymbol, Name name, Predicate<Symbol> filter);
}
