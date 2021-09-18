package io.github.vipcxj.jasync.core.java9.javac.utils;

import com.google.auto.service.AutoService;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Name;
import io.github.vipcxj.jasync.core.javac.utils.spi.SymbolHelper;

import java.util.function.Predicate;

@AutoService(SymbolHelper.class)
public class SymbolHelperImpl implements SymbolHelper {

    @Override
    public Symbol getSymbol(Symbol.TypeSymbol classSymbol, Name name, Predicate<Symbol> filter) {
        if (filter != null) {
            return classSymbol.members().findFirst(name, filter::test);
        } else {
            return classSymbol.members().findFirst(name);
        }
    }

    @Override
    public int supportVersion() {
        return 9;
    }
}
