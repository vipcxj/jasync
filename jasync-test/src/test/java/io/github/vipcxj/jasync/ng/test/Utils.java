package io.github.vipcxj.jasync.ng.test;

import io.github.vipcxj.jasync.ng.spec.exceptions.JAsyncWrapException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;

public class Utils {
    public static void assertError(Class<? extends Throwable> type, Executable executable) {
        Assertions.assertThrows(type, () -> {
            try {
                executable.execute();
            } catch (JAsyncWrapException e) {
                throw e.getCause();
            }
        });
    }
}
