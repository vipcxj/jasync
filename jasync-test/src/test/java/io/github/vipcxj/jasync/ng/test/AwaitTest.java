package io.github.vipcxj.jasync.ng.test;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AwaitTest {

    private JPromise<Integer> zero() {
        return JPromise.just(0);
    }

    private JPromise<Integer> one() {
        return JPromise.just(1);
    }

    private JPromise<Integer> two() {
        return JPromise.just(2);
    }

    private JPromise<Integer> three() {
        return JPromise.just(3);
    }

    private JPromise<Integer> plus(int a, int b) {
        return JPromise.just(a + b);
    }

    @SuppressWarnings("SameParameterValue")
    private JPromise<Integer> mul(int a , int b) {
        return JPromise.just(a * b);
    }

    @Async
    private JPromise<String> simpleAwait() {
        JPromise<String> helloWorld = JPromise.just("hello world");
        return JPromise.just(helloWorld.await());
    }

    @Test
    public void testSimpleAwait() throws InterruptedException {
        Assertions.assertEquals("hello world", simpleAwait().block());
    }

    private int nestedNoAwait1() {
        return 1 + (1 + (3 + 2)) + 3 * 4;
    }

    private JPromise<Integer> nestedAwait1() {
        int res = 1 + plus(one().await(), plus(3, two().await()).await()).await()
                + zero().await()
                +  mul(three().await(), 4).await();
        return JPromise.just(res);
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
    private JPromise<Integer> nestedAwait2() {
        int i = 0;
        i = plus(++i, two().await() * ++i).await();
        return JPromise.just(i);
    }

    @Test
    public void testNested2() throws InterruptedException {
        Assertions.assertEquals(nestedNoAwait2(), nestedAwait2().block());
    }
}
