package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.spec.JPromise2;
import io.github.vipcxj.jasync.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AwaitTest {

    private JPromise2<Integer> zero() {
        return JPromise2.just(0);
    }

    private JPromise2<Integer> one() {
        return JPromise2.just(1);
    }

    private JPromise2<Integer> two() {
        return JPromise2.just(2);
    }

    private JPromise2<Integer> three() {
        return JPromise2.just(3);
    }

    private JPromise2<Integer> plus(int a, int b) {
        return JPromise2.just(a + b);
    }

    @SuppressWarnings("SameParameterValue")
    private JPromise2<Integer> mul(int a , int b) {
        return JPromise2.just(a * b);
    }

    @Async
    private JPromise2<String> simpleAwait() {
        JPromise2<String> helloWorld = JPromise2.just("hello world");
        return JPromise2.just(helloWorld.await());
    }

    @Test
    public void testSimpleAwait() throws InterruptedException {
        Assertions.assertEquals("hello world", simpleAwait().block());
    }

    private int nestedNoAwait1() {
        return 1 + (1 + (3 + 2)) + 3 * 4;
    }

    @Async
    private JPromise2<Integer> nestedAwait1() {
        int res = 1 + plus(one().await(), plus(3, two().await()).await()).await()
                + zero().await()
                +  mul(three().await(), 4).await();
        return JPromise2.just(res);
    }

    @Test
    public void testNested1() throws InterruptedException {
        Assertions.assertEquals(nestedNoAwait1(), nestedAwait1().block());
    }

    private int nestedNoAwait2() {
        int i = 0;
        return ++i + 2 * ++i;
    }

    @Async
    private JPromise2<Integer> nestedAwait2() {
        int i = 0;
        i = plus(++i, two().await() * ++i).await();
        return JPromise2.just(i);
    }

    @Test
    public void testNested2() throws InterruptedException {
        Assertions.assertEquals(nestedNoAwait2(), nestedAwait2().block());
    }
}
