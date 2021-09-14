package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.spec.JAsync;
import io.github.vipcxj.jasync.spec.JPromise;
import io.github.vipcxj.jasync.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BlockTest {

    @Async
    private JPromise<Long> block1() {
        long out = 0;
        {
            long one = JAsync.just(1).await();
            out += one;
        }
        {
            out += JAsync.just(2).await();
        }
        return JAsync.just(out);
    }

    @Test
    public void testBlock1() {
        Assertions.assertEquals(3, block1().block());
    }
}
