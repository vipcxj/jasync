package io.github.vipcxj.jasync.ng.test;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LongAndDoubleTest {

    private JPromise<Long> simpleLong1(long n) {
        long a = JPromise.just(n).await();
        return JPromise.just(a);
    }

    @Test
    void testSimpleLong1() throws InterruptedException {
        Assertions.assertEquals(1L, simpleLong1(1L).block());
        Assertions.assertEquals(2L, simpleLong1(2L).block());
    }

    private JPromise<Long> simpleLong2(long n) {
        long a = n + 1;
        long b = a + JPromise.just(1L).await();
        return JPromise.just(a + b);
    }

    @Test
    void testSimpleLong2() throws InterruptedException {
        Assertions.assertEquals(5L, simpleLong2(1L).block());
        Assertions.assertEquals(7L, simpleLong2(2L).block());
    }
}
