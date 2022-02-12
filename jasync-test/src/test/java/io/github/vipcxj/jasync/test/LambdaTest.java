package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.spec.JPromise2;
import io.github.vipcxj.jasync.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

public class LambdaTest {

    @Async
    public JPromise2<String> awaitInLambda() {
        Supplier<JPromise2<String>> supplier = () -> {
            String helloWorld = JPromise2.just("Hello World").await();
            return JPromise2.just(helloWorld);
        };
        return supplier.get();
    }

    @Test
    public void test1() throws InterruptedException {
        Assertions.assertEquals("Hello World", awaitInLambda().block());
    }
}
