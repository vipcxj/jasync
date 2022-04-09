package io.github.vipcxj.jasync.ng.test;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.function.*;

public class AnonymousClassTest {

    @Async
    private JPromise<String> validAwaitInAnonymousClassShouldTransform(String input) {
        //noinspection Convert2Lambda
        Supplier<JPromise<String>> supplier = new Supplier<JPromise<String>>() {
            @Override
            public JPromise<String> get() {
                String helloWorld = JPromise.just("Hello World").await();
                return JPromise.just(helloWorld + input);
            }
        };
        return supplier.get();
    }

    @Test
    public void test1() throws InterruptedException {
        Assertions.assertEquals("Hello World.", validAwaitInAnonymousClassShouldTransform(".").block());
        Assertions.assertEquals("Hello World!", validAwaitInAnonymousClassShouldTransform("!").block());
    }
}
