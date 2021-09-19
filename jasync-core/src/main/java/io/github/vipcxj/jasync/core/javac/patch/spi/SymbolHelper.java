package io.github.vipcxj.jasync.core.javac.patch.spi;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Name;

import java.util.function.Predicate;

public interface SymbolHelper {

    Symbol getSymbol(Symbol.TypeSymbol classSymbol, Name name, Predicate<Symbol> filter);

    int supportVersion();
}
