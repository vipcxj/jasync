package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.spec.JAsync;
import io.github.vipcxj.jasync.spec.JPromise;
import io.github.vipcxj.jasync.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AwaitTest {

    private JPromise<Integer> zero() {
        return JAsync.just(0);
    }

    private JPromise<Integer> one() {
        return JAsync.just(1);
    }

    private JPromise<Integer> two() {
        return JAsync.just(2);
    }

    private JPromise<Integer> three() {
        return JAsync.just(3);
    }

    private JPromise<Integer> plus(int a, int b) {
        return JAsync.just(a + b);
    }

    private JPromise<Integer> mul(int a , int b) {
        return JAsync.just(a * b);
    }

    @Async
    private JPromise<String> simpleAwait() {
        JPromise<String> helloWorld = JAsync.just("hello world");
        return JAsync.just(helloWorld.await());
    }

    @Test
    public void testSimpleAwait() {
        Assertions.assertEquals("hello world", simpleAwait().block());;
    }

    private int nestedNoAwait1() {
        return 1 + (1 + (3 + 2)) + 3 * 4;
    }

    @Async
    private JPromise<Integer> nestedAwait1() {
        int res = 1 + plus(one().await(), plus(3, two().await()).await()).await()
                + zero().await()
                +  mul(three().await(), 4).await();
        return JAsync.just(res);
    }

    @Test
    public void testNested1() {
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
        return JAsync.just(i);
    }

    @Test
    public void testNested2() {
        Assertions.assertEquals(nestedNoAwait2(), nestedAwait2().block());
    }
}
