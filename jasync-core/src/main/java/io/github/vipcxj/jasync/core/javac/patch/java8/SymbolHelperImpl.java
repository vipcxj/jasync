package io.github.vipcxj.jasync.core.javac.patch.java8;

import com.google.auto.service.AutoService;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Name;
import io.github.vipcxj.jasync.core.javac.patch.spi.SymbolHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Predicate;

@AutoService(SymbolHelper.class)
public class SymbolHelperImpl implements SymbolHelper {

    private static final Logger logger = LogManager.getLogger();

    @Override
    public Symbol getSymbol(Symbol.TypeSymbol classSymbol, Name name, Predicate<Symbol> filter) {
        logger.traceEntry("get method symbol of \"{}\" from class \"{}\"", name, classSymbol);
        if (filter != null) {
            return logger.traceExit(
                    classSymbol.members().getElementsByName(name, filter::test).iterator().next()
            );
        } else {
            return logger.traceExit(
                    classSymbol.members().getElementsByName(name).iterator().next()
            );
        }
    }

    @Override
    public int supportVersion() {
        return 8;
    }
}
