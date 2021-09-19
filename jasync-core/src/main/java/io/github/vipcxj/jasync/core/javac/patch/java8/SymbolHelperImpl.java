package io.github.vipcxj.jasync.core.javac.patch.java8;

import com.google.auto.service.AutoService;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Name;
import io.github.vipcxj.jasync.core.javac.patch.spi.SymbolHelper;

import java.util.function.Predicate;

@AutoService(SymbolHelper.class)
public class SymbolHelperImpl implements SymbolHelper {

    @Override
    public Symbol getSymbol(Symbol.TypeSymbol classSymbol, Name name, Predicate<Symbol> filter) {
        if (filter != null) {
            return classSymbol.members().getElementsByName(name, filter::test).iterator().next();
        } else {
            return classSymbol.members().getElementsByName(name).iterator().next();
        }
    }

    @Override
    public int supportVersion() {
        return 8;
    }
}
