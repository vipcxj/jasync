package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.spec.JPromise2;
import io.github.vipcxj.jasync.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StaticTest {

    @Async
    private static JPromise2<String> helloWorld() {
        String message = "hello" + JPromise2.just(" world").await();
        return JPromise2.just(message);
    }

    @Test
    public void testHelloWorld() throws InterruptedException {
        Assertions.assertEquals("hello world", helloWorld().block());
    }
}
