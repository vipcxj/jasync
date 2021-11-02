package io.github.vipcxj.jasync.spec.catcher;

import io.github.vipcxj.jasync.spec.JPromise2;
import io.github.vipcxj.jasync.spec.functional.JAsyncCatchFunction1;

import java.util.Arrays;
import java.util.List;

public class Catchers {

    @SafeVarargs
    public static <T> List<Catcher<?, T>> of(Catcher<?, T>...catchers) {
        return Arrays.asList(catchers);
    }

    /** @noinspection unchecked */
    @SafeVarargs
    public static <T> JAsyncCatchFunction1<Throwable, T> of(Catcher2<?, T>...catchers) {
        return (error, context) -> {
            for (Catcher2<?, T> catcher : catchers) {
                if (catcher.match(error)) {
                    //noinspection rawtypes
                    return ((JAsyncCatchFunction1) catcher.getReject()).apply(error, context);
                }
            }
            return JPromise2.error(error);
        };
    }
}
