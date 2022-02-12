package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.spec.JPromise2;
import io.github.vipcxj.jasync.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.function.*;

public class AnonymousClassTest {

    @Async
    private JPromise2<String> validAwaitInAnonymousClassShouldTransform(String input) {
        //noinspection Convert2Lambda
        Supplier<JPromise2<String>> supplier = new Supplier<JPromise2<String>>() {
            @Override
            public JPromise2<String> get() {
                String helloWorld = JPromise2.just("Hello World").await();
                return JPromise2.just(helloWorld + input);
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
