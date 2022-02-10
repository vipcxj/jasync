package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.spec.JPromise2;
import io.github.vipcxj.jasync.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class WhileTest {

    @Async
    public JPromise2<Integer> sum1(int to) {
        int sum = 0;
        while (sum < to) {
            sum += JPromise2.just(1).await();
        }
        return JPromise2.just(sum);
    }

    @Async
    public JPromise2<Integer> sum2(int to) {
        int sum = 0;
        while (sum < JPromise2.just(to).await()) {
            sum += JPromise2.just(1).await();
        }
        return JPromise2.just(sum);
    }

    @Async
    public JPromise2<Integer> multi1(int a, int b) {
        int res = 0;
        int i = 0, j = 0;
        while (i++ < a) {
            while (j++ < b) {
                res += JPromise2.just(1).await();
            }
            j = 0;
        }
        return JPromise2.just(res);
    }

    @Async(debug = true)
    public JPromise2<Integer> multi2(int a, int b) {
        int res = 0;
        int i = 0, j = 0;
        while (i++ < JPromise2.just(a).await()) {
            while (j++ < JPromise2.just(b).await()) {
                res += JPromise2.just(1).await();
            }
            j = 0;
        }
        return JPromise2.just(res);
    }

    @Test
    public void test1() throws InterruptedException {
        Assertions.assertEquals(2000000, sum1(2000000).block());
    }

    @Test
    public void test2() throws InterruptedException {
        Assertions.assertEquals(100000, sum2(100000).block());
    }

    @Test
    public void test3() throws InterruptedException {
        Assertions.assertEquals(123 * 321, multi1(123, 321).block());
    }

    @Test
    public void test4() throws InterruptedException {
        Assertions.assertEquals(123 * 321, multi2(123, 321).block());
    }

}
