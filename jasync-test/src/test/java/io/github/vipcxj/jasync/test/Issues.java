package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.spec.JAsync;
import io.github.vipcxj.jasync.spec.JPromise;
import io.github.vipcxj.jasync.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class Issues {

    @Async
    public <G> JPromise<G> get(Class<G> format) {
        Long data = JAsync.just(1L).await();
        return JAsync.just(format.cast(data));
    }

    @Test
    public void issue4() {
        Assertions.assertEquals(1L, get(Number.class).block());
    }

    @Async
    public JPromise<Integer> test2(int input1, int input2) {
        int one = JAsync.just(1).await();
        Supplier<Integer> arg = () -> input1;
        Supplier<Integer> sum = () -> one + arg.get() + input2;
        return JAsync.just(sum.get());
    }

    @Test
    public void issue5() {
        Assertions.assertEquals(4, test2(1, 2).block());
        Assertions.assertEquals(3, test2(0, 2).block());
        Assertions.assertEquals(7, test2(3, 3).block());
    }

    @Async
    public JPromise<Long> test3() {
        List<Long> list = new ArrayList<>();
        list.add(1L);
        list.add(2L);
        list.add(3L);
        list.add(4L);

        long sum = 0;
        for (Long l : list) {
            if (sum == 3) {
                sum++;
                continue;
            }
            // throw io.github.vipcxj.jasync.spec.ContinueException
            sum += JAsync.just(l).await();
        }

        return JAsync.just(sum);
    }

    @Test
    public void issue6() {
        Assertions.assertEquals(8, test3().block());
    }

    private List<String> getA(String a) {
        return Collections.singletonList(a);
    }

    @SuppressWarnings("unused")
    @Async
    private JPromise<String> testIssue7() {
        String a = "定义a";
        try {
            a = "修改a";
        } catch (Exception e) {
            for (String l : getA(a)) {
                JAsync.just().await();
            }
        }
        return JAsync.just(a);
    }

    @Test
    public void issue7() {
        Assertions.assertEquals("修改a", testIssue7().block());
    }
}
