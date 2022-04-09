package io.github.vipcxj.jasync.ng.test;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StaticTest {

    @Async
    private static JPromise<String> helloWorld() {
        String message = "hello" + JPromise.just(" world").await();
        return JPromise.just(message);
    }

    @Test
    public void testHelloWorld() throws InterruptedException {
        Assertions.assertEquals("hello world", helloWorld().block());
    }
}
