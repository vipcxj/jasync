package io.github.vipcxj.jasync.ng.test;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.annotations.Async;
import io.github.vipcxj.jasync.ng.spec.functional.JAsyncPromiseSupplier0;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LambdaTest {

    @Async
    public JPromise<String> awaitInLambda() {
        JAsyncPromiseSupplier0<String> supplier = () -> {
            String helloWorld = JPromise.just("Hello World").await();
            return JPromise.just(helloWorld);
        };
        try {
            return supplier.get();
        } catch (Throwable throwable) {
            return JPromise.error(throwable);
        }
    }

    @Test
    public void test1() throws InterruptedException {
        Assertions.assertEquals("Hello World", awaitInLambda().block());
    }
}
