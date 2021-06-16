package io.github.vipcxj.jasync.spec.switchexpr;

import java.util.Arrays;
import java.util.List;

public class Cases {

    public static List<IntCase> of(IntCase... cases) {
        return Arrays.asList(cases);
    }

    public static List<StringCase> of(StringCase... cases) {
        return Arrays.asList(cases);
    }

    public static <E extends Enum<E>> List<EnumCase<E>> of(EnumCase<E>... cases) {
        return Arrays.asList(cases);
    }
}
