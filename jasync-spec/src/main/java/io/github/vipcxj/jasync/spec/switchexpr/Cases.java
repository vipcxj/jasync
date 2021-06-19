package io.github.vipcxj.jasync.spec.switchexpr;

import java.util.Arrays;
import java.util.List;

public class Cases {

    @SafeVarargs
    public static <C> List<ICase<C>> of(ICase<C>... cases) {
        return Arrays.asList(cases);
    }
}
