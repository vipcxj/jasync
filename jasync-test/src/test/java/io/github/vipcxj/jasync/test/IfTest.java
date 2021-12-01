package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.spec.JPromise2;
import io.github.vipcxj.jasync.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IfTest {

    @Async
    private JPromise2<String> ifTest1(int toTest) {
        if (JPromise2.just(3).await().equals(toTest)) {
            return JPromise2.just("yes");
        }
        return JPromise2.just("no");
    }

    @Test
    public void testIf1() {
        Assertions.assertEquals("yes", ifTest1(3).block());
        Assertions.assertEquals("no", ifTest1(4).block());
    }

    @Async
    private JPromise2<String> ifTest2(int toTest) {
        JPromise2<Integer> three = JPromise2.just(3);
        if (three.await().equals(toTest)) {
            return JPromise2.just("yes");
        } else {
            return JPromise2.just("no");
        }
    }

    @Test
    public void testIf2() {
        Assertions.assertEquals("yes", ifTest2(3).block());
        Assertions.assertEquals("no", ifTest2(4).block());
    }

    @Async
    private JPromise2<String> ifTest3(String message) {
        if (message != null) {
            String hello = JPromise2.just("hello ").await();
            return JPromise2.just(hello + message);
        }
        return JPromise2.empty();
    }

    @Test
    public void testIf3() {
        Assertions.assertEquals("hello world", ifTest3("world").block());
        //noinspection ConstantConditions
        Assertions.assertNull(ifTest3(null).block());
    }

    @Async
    private JPromise2<String> ifTest4(int i) {
        if (i == 0) {
            return JPromise2.just("0");
        } else if (i == JPromise2.just(1).await()) {
            return JPromise2.just("1");
        } else if (JPromise2.just(i).await() == 2) {
            return JPromise2.just("2");
        } else if (JPromise2.just(i).await().equals(JPromise2.just(3).await())) {
            return JPromise2.just("3");
        } else if (i == 4) {
            return JPromise2.just("4");
        } else {
            return JPromise2.just("5");
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

    private JPromise2<Integer> ifTest5(int i) {
        int result = 0;
        if (i == 0) result = JPromise2.just(i).await() + 1;
        if (i == 1) result = JPromise2.just(i).await() + 2;
        return JPromise2.just(result);
    }

    @Test
    public void testIf5() {
        Assertions.assertEquals(1, ifTest5(0).block());
        Assertions.assertEquals(3, ifTest5(1).block());
        Assertions.assertEquals(0, ifTest5(2).block());
    }

}
