package io.github.vipcxj.jasync.core.java17.javac.utils;

import com.google.auto.service.AutoService;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Name;
import io.github.vipcxj.jasync.core.hack.Utils;
import io.github.vipcxj.jasync.core.javac.utils.spi.SymbolHelper;

import java.util.function.Predicate;

@AutoService(SymbolHelper.class)
public class SymbolHelperImpl implements SymbolHelper {

    static {
        Utils.addOpensForLombok(SymbolHelperImpl.class, new String[] {
                "com.sun.tools.javac.code",
                "com.sun.tools.javac.util"
        });
    }

    @Override
    public Symbol getSymbol(Symbol.TypeSymbol classSymbol, Name name, Predicate<Symbol> filter) {
        if (filter != null) {
            return classSymbol.members().findFirst(name, filter);
        } else {
            return classSymbol.members().findFirst(name);
        }
    }

    @Override
    public int supportVersion() {
        return 17;
    }
}
