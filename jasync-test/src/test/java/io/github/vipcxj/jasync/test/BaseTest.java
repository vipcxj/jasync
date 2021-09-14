package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.spec.JPromise;
import io.github.vipcxj.jasync.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BaseTest {

    @Async
    private JPromise<Void> returnNull() {
        return null;
    }

    @Test
    public void testReturnNull() {
        //noinspection ConstantConditions
        Assertions.assertNull(returnNull().block());
    }

    @Async
    private JPromise<Integer> asyncMethodInnerClassReturnNull() {
        class Tmp {
            @Async
            private JPromise<Integer> run() {
                return null;
            }
        }
        return new Tmp().run();
    }

    @Test
    public void testInnerClassReturnNull() {
        Assertions.assertNull(asyncMethodInnerClassReturnNull().block());
    }
}
