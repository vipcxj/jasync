package io.github.vipcxj.jasync.ng.test;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.annotations.Async;
import io.github.vipcxj.jasync.ng.spec.functional.JAsyncPromiseSupplier0;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AnonymousClassTest {

    @Async
    private JPromise<String> validAwaitInAnonymousClassShouldTransform(String input) {
        //noinspection Convert2Lambda
        JAsyncPromiseSupplier0<String> supplier = new JAsyncPromiseSupplier0<String>() {
            @Override
            public JPromise<String> get() {
                String helloWorld = JPromise.just("Hello World").await();
                return JPromise.just(helloWorld + input);
            }
        };
        try {
            return supplier.get();
        } catch (Throwable throwable) {
            return JPromise.error(throwable);
        }
    }

    @Test
    public void test1() throws InterruptedException {
        Assertions.assertEquals("Hello World.", validAwaitInAnonymousClassShouldTransform(".").block());
        Assertions.assertEquals("Hello World!", validAwaitInAnonymousClassShouldTransform("!").block());
    }
}
