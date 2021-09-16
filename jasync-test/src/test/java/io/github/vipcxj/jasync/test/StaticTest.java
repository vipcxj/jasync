package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.spec.JAsync;
import io.github.vipcxj.jasync.spec.JPromise;
import io.github.vipcxj.jasync.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StaticTest {

    @Async
    private static JPromise<String> helloWorld() {
        String message = "hello" + JAsync.just(" world").await();
        return JAsync.just(message);
    }

    @Test
    public void testHelloWorld() {
        Assertions.assertEquals("hello world", helloWorld().block());
    }
}
