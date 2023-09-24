package io.github.vipcxj.jasync.ng.test;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IfTest {

    @Async(verify = true)
    private JPromise<String> ifTest1(int toTest) {
        if (JPromise.just(3).await().equals(toTest)) {
            return JPromise.just("yes");
        }
        return JPromise.just("no");
    }

    @Test
    public void testIf1() throws InterruptedException {
        Assertions.assertEquals("yes", ifTest1(3).block());
        Assertions.assertEquals("no", ifTest1(4).block());
    }

    @Async(verify = true)
    private JPromise<String> ifTest2(int toTest) {
        JPromise<Integer> three = JPromise.just(3);
        if (three.await().equals(toTest)) {
            return JPromise.just("yes");
        } else {
            return JPromise.just("no");
        }
    }

    @Test
    public void testIf2() throws InterruptedException {
        Assertions.assertEquals("yes", ifTest2(3).block());
        Assertions.assertEquals("no", ifTest2(4).block());
    }

    @Async(verify = true)
    private JPromise<String> ifTest3(String message) {
        if (message != null) {
            String hello = JPromise.just("hello ").await();
            return JPromise.just(hello + message);
        }
        return JPromise.empty();
    }

    @Test
    public void testIf3() throws InterruptedException {
        Assertions.assertEquals("hello world", ifTest3("world").block());
        Assertions.assertNull(ifTest3(null).block());
    }

    @Async(verify = true)
    private JPromise<String> ifTest4(int i) {
        if (i == 0) {
            return JPromise.just("0");
        } else if (i == JPromise.just(1).await()) {
            return JPromise.just("1");
        } else if (JPromise.just(i).await() == 2) {
            return JPromise.just("2");
        } else if (JPromise.just(i).await().equals(JPromise.just(3).await())) {
            return JPromise.just("3");
        } else if (i == 4) {
            return JPromise.just("4");
        } else {
            return JPromise.just("5");
        }
    }

    @Test
    public void testIf4() throws InterruptedException {
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

    @Async(verify = true)
    private JPromise<Integer> ifTest5(int i) {
        int result = 0;
        if (i == 0) result = JPromise.just(i).await() + 1;
        if (i == 1) result = JPromise.just(i).await() + 2;
        return JPromise.just(result);
    }

    @Test
    public void testIf5() throws InterruptedException {
        Assertions.assertEquals(1, ifTest5(0).block());
        Assertions.assertEquals(3, ifTest5(1).block());
        Assertions.assertEquals(0, ifTest5(2).block());
    }

}
