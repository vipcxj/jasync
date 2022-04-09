package io.github.vipcxj.jasync.ng.test;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

public class LambdaTest {

    @Async
    public JPromise<String> awaitInLambda() {
        Supplier<JPromise<String>> supplier = () -> {
            String helloWorld = JPromise.just("Hello World").await();
            return JPromise.just(helloWorld);
        };
        return supplier.get();
    }

    @Test
    public void test1() throws InterruptedException {
        Assertions.assertEquals("Hello World", awaitInLambda().block());
    }
}
