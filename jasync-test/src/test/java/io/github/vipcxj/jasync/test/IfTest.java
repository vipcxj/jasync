package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.spec.JAsync;
import io.github.vipcxj.jasync.spec.JPromise;
import io.github.vipcxj.jasync.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IfTest {

    @Async
    private JPromise<String> ifTest1(int toTest) {
        if (JAsync.just(3).await().equals(toTest)) {
            return JAsync.just("yes");
        }
        return JAsync.just("no");
    }

    @Test
    public void testIf1() {
        Assertions.assertEquals("yes", ifTest1(3).block());
        Assertions.assertEquals("no", ifTest1(4).block());
    }

    @Async
    private JPromise<String> ifTest2(int toTest) {
        JPromise<Integer> three = JAsync.just(3);
        if (three.await().equals(toTest)) {
            return JAsync.just("yes");
        } else {
            return JAsync.just("no");
        }
    }

    @Test
    public void testIf2() {
        Assertions.assertEquals("yes", ifTest2(3).block());
        Assertions.assertEquals("no", ifTest2(4).block());
    }

    @Async
    private JPromise<String> ifTest3(String message) {
        if (message != null) {
            String hello = JAsync.just("hello ").await();
            return JAsync.just(hello + message);
        }
        return null;
    }

    @Test
    public void testIf3() {
        Assertions.assertEquals("hello world", ifTest3("world").block());
        //noinspection ConstantConditions
        Assertions.assertNull(ifTest3(null).block());
    }

    @Async
    private JPromise<String> ifTest4(int i) {
        if (i == 0) {
            return JAsync.just("0");
        } else if (i == JAsync.just(1).await()) {
            return JAsync.just("1");
        } else if (JAsync.just(i).await() == 2) {
            return JAsync.just("2");
        } else if (JAsync.just(i).await() == JAsync.just(3).await()) {
            return JAsync.just("3");
        } else if (i == 4) {
            return JAsync.just("4");
        } else {
            return JAsync.just("5");
        }
    }

    @Test
    public void testIf4() {
        Assertions.assertEquals("5", ifTest4(-2).block());
        Assertions.assertEquals("5", ifTest4(-1).block());
        Assertions.assertEquals("0", ifTest4(0).block());
        Assertions.assertEquals("1", ifTest4(1).block());
        Assertions.assertEquals("2", ifTest4(2).block());
        Assertions.assertEquals("3", ifTest4(3).block());
        Assertions.assertEquals("4", ifTest4(4).block());
        Assertions.assertEquals("5", ifTest4(5).block());
        Assertions.assertEquals("5", ifTest4(6).block());
    }

}
