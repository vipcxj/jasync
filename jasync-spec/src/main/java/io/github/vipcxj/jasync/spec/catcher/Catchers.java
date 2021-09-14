package io.github.vipcxj.jasync.spec.catcher;

import java.util.Arrays;
import java.util.List;

public class Catchers {

    @SafeVarargs
    public static <T> List<Catcher<?, T>> of(Catcher<?, T>...catchers) {
        return Arrays.asList(catchers);
    }
}
