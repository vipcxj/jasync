package io.github.vipcxj.jasync.patch.javac.java9;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Name;
import io.github.vipcxj.jasync.utils.hack.Utils;
import io.github.vipcxj.jasync.utils.patch.IPatch;

import java.util.function.Predicate;

public class Patch implements IPatch {

    static {
        Utils.addOpensForLombok(Patch.class, new String[] {
                "com.sun.tools.javac.code",
                "com.sun.tools.javac.util"
        });
    }

    @Override
    public Symbol getSymbol(Symbol.TypeSymbol classSymbol, Name name, Predicate<Symbol> filter) {
        if (filter != null) {
            return classSymbol.members().findFirst(name, filter::test);
        } else {
            return classSymbol.members().findFirst(name);
        }
    }
}
