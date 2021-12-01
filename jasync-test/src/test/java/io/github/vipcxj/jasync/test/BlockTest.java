package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.spec.JPromise2;
import io.github.vipcxj.jasync.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BlockTest {

    @Async
    private JPromise2<Long> block1() {
        long out = 0;
        {
            long one = JPromise2.just(1).await();
            out += one;
        }
        {
            out += JPromise2.just(2).await();
        }
        return JPromise2.just(out);
    }

    @Test
    public void testBlock1() {
        Assertions.assertEquals(3, block1().block());
    }
}
