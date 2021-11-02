package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.spec.JAsync;
import io.github.vipcxj.jasync.spec.JPromise;
import io.github.vipcxj.jasync.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
    public JPromise<Integer> test2(int input) {
        int one = JAsync.just(1).await();
        Supplier<Integer> sum = () -> one + input;
        return JAsync.just(sum.get());
    }

    @Test
    public void issue5() {
        Assertions.assertEquals(2, test2(1).block());
        Assertions.assertEquals(3, test2(2).block());
        Assertions.assertEquals(4, test2(3).block());
    }
}
