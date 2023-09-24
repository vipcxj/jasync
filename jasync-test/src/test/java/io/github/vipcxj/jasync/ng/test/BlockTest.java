package io.github.vipcxj.jasync.ng.test;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BlockTest {

    @Async
    private JPromise<Long> block1() {
        long out = 0;
        {
            long one = JPromise.just(1).await();
            out += one;
        }
        {
            out += JPromise.just(2).await();
        }
        return JPromise.just(out);
    }

    @Test
    public void testBlock1() throws InterruptedException {
        Assertions.assertEquals(3, block1().block());
    }
}
