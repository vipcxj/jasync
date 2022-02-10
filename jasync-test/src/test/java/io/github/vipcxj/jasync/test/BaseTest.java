package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.spec.JPromise2;
import io.github.vipcxj.jasync.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BaseTest {

    @Async
    private JPromise2<Void> returnNull() {
        return JPromise2.empty();
    }

    @Test
    public void testReturnNull() throws InterruptedException {
        //noinspection ConstantConditions
        Assertions.assertNull(returnNull().block());
    }

    @Async
    private JPromise2<Integer> asyncMethodInnerClassReturnNull() {
        class Tmp {
            @Async
            private JPromise2<Integer> run() {
                return JPromise2.empty();
            }
        }
        return new Tmp().run();
    }

    @Test
    public void testInnerClassReturnNull() throws InterruptedException {
        Assertions.assertNull(asyncMethodInnerClassReturnNull().block());
    }
}
