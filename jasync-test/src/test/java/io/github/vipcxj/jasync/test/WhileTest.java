package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.spec.JAsync;
import io.github.vipcxj.jasync.spec.JPromise;
import io.github.vipcxj.jasync.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class WhileTest {

    @Async
    public JPromise<Integer> sum1(int to) {
        int sum = 0;
        while (sum < to) {
            sum += JAsync.just(1).await();
        }
        return JAsync.just(sum);
    }

    @Async
    public JPromise<Integer> sum2(int to) {
        int sum = 0;
        while (sum < JAsync.just(to).await()) {
            sum += JAsync.just(1).await();
        }
        return JAsync.just(sum);
    }

    @Async
    public JPromise<Integer> multi1(int a, int b) {
        int res = 0;
        int i = 0, j = 0;
        while (i++ < a) {
            while (j++ < b) {
                res += JAsync.just(1).await();
            }
            j = 0;
        }
        return JAsync.just(res);
    }

    @Async(debug = true)
    public JPromise<Integer> multi2(int a, int b) {
        int res = 0;
        int i = 0, j = 0;
        while (i++ < JAsync.just(a).await()) {
            while (j++ < JAsync.just(b).await()) {
                res += JAsync.just(1).await();
            }
            j = 0;
        }
        return JAsync.just(res);
    }

    @Test
    public void test1() {
        Assertions.assertEquals(2000000, sum1(2000000).block());
    }

    @Test
    public void test2() {
        Assertions.assertEquals(100000, sum2(100000).block());
    }

    @Test
    public void test3() {
        Assertions.assertEquals(123 * 321, multi1(123, 321).block());
    }

    @Test
    public void test4() {
        Assertions.assertEquals(123 * 321, multi2(123, 321).block());
    }

}
