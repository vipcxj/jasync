package io.github.vipcxj.jasync.ng.test;

import io.github.vipcxj.jasync.ng.runtime.promise.ValuePromise;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ATest {

    private JPromise<Integer> one() {
        return JPromise.just(1);
    }

    private JPromise<Integer> two() {
        return JPromise.just(2);
    }

    @Async
    public JPromise<Integer> a(int input) throws InterruptedException {
        Number a = 5;
        @LocalAnn
        Integer one = one().await();
        @LocalAnn(a = true)
        Integer two = two().await();
        int res = 0;
        label: for (int i = 0; i < 10; ++i) {
            a = a.longValue() - 1;
            for (int j = 0; j < 10; ++j) {
                if (one == 1) {
                    if (two == 2) {
                        a = a.intValue() + 1;
                        JPromise<Integer> subPromise = new SubPromise<>(3);
                        res = one + two + subPromise.await();
                    } else if (two == 3) {
                        continue label;
                    } else {
                        break;
                    }
                }
            }
        }
        return JPromise.just(res);
    }

    @Test
    public void testA() throws InterruptedException {
        Assertions.assertEquals(6, a(1).block());
    }

    @Async
    @SuppressWarnings({"BoxingBoxedValue", "CachedNumberConstructorCall"})
    public JPromise<Integer> aaa() throws InterruptedException {
        Integer one = new Integer(JPromise.just(1).await());
        Integer two = new Integer(JPromise.just(2).await());
        return JPromise.just(one + two);
    }

    @Test
    public void testAAA() throws InterruptedException {
        Assertions.assertEquals(3, aaa().block());
    }

    @Async
    public JPromise<Integer> aa() throws InterruptedException {
        Integer one = JPromise.just(1).await();
        Integer two = JPromise.just(2).await();
        return JPromise.just(one + two);
    }

    @Test
    public void testAA() throws InterruptedException {
        Assertions.assertEquals(3, aa().block());
    }

    public JPromise<Integer> b() {
        return one().then(one -> two().then(two -> {
            int res = one + two;
            return JPromise.just(res);
        }));
    }

    public int c() {
        int a = 0;
        int c = 0;
        {
            int b = 1;
            c += b;
        }
        return c;
    }

    static class SubPromise<T> extends ValuePromise<T> {

        public SubPromise(T value) {
            super(value);
        }
    }
}
