package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.spec.JAsync;
import io.github.vipcxj.jasync.spec.JPromise;
import io.github.vipcxj.jasync.spec.annotations.Async;
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
                String helloWorld = JAsync.just("Hello World").await();
                return JAsync.just(helloWorld + input);
            }
        };
        return supplier.get();
    }

    @Test
    public void test1() {
        Assertions.assertEquals("Hello World.", validAwaitInAnonymousClassShouldTransform(".").block());
        Assertions.assertEquals("Hello World!", validAwaitInAnonymousClassShouldTransform("!").block());
    }

    @Async
    private JPromise<Integer> invalidAwaitInAnonymousClassShouldNotTransform(int input1, int input2) {
        //noinspection Convert2Lambda
        IntUnaryOperator sum = new IntUnaryOperator() {

            @Override
            public int applyAsInt(int operand) {
                int v = JAsync.just(input1).await();
                return v + operand;
            }
        };
        return JAsync.just(sum.applyAsInt(input2));
    }

    @Test
    public void test2() {
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> invalidAwaitInAnonymousClassShouldNotTransform(1, 2).block(),
                () -> {
                    try {
                        JAsync.just().await();
                    } catch (UnsupportedOperationException e) {
                        return e.getMessage();
                    }
                    return null;
                });
        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> invalidAwaitInAnonymousClassShouldNotTransform(2, 3).block(),
                () -> {
                    try {
                        JAsync.just().await();
                    } catch (UnsupportedOperationException e) {
                        return e.getMessage();
                    }
                    return null;
                });
    }
}
