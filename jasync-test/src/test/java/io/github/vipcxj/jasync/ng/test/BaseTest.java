package io.github.vipcxj.jasync.ng.test;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BaseTest {

    @Async
    private JPromise<Void> returnNull() {
        return JPromise.empty();
    }

    @Test
    public void testReturnNull() throws InterruptedException {
        //noinspection ConstantConditions
        Assertions.assertNull(returnNull().block());
    }

    @Async
    private JPromise<Integer> asyncMethodInnerClassReturnNull() {
        class Tmp {
            @Async
            private JPromise<Integer> run() {
                return JPromise.empty();
            }
        }
        return new Tmp().run();
    }

    @Test
    public void testInnerClassReturnNull() throws InterruptedException {
        Assertions.assertNull(asyncMethodInnerClassReturnNull().block());
    }
}
