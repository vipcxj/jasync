package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.spec.JAsync;
import io.github.vipcxj.jasync.spec.JPromise;
import io.github.vipcxj.jasync.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

public class LambdaTest {

    @Async
    public JPromise<String> awaitInLambdaShouldNotTransform() {
        Supplier<JPromise<String>> supplier = () -> {
            String helloWorld = JAsync.just("Hello World").await();
            return JAsync.just(helloWorld);
        };
        return supplier.get();
    }

    @Test
    public void test1() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> awaitInLambdaShouldNotTransform().block(), () -> {
            try {
                JAsync.just().await();
            } catch (UnsupportedOperationException e) {
                return e.getMessage();
            }
            return null;
        });
    }
}
