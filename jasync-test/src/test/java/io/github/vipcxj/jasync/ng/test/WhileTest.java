package io.github.vipcxj.jasync.ng.test;

import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JHandle;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.concurrent.TimeUnit;

public class WhileTest {

    @Async(debugId = "sum1", logOriginalByteCode = Async.BYTE_CODE_OPTION_FULL_SUPPORT)
    public JPromise<Integer> sum1(int to) throws InterruptedException {
        int sum = 0;
        while (sum < to) {
            sum += JPromise.just(1).await();
        }
        return JPromise.just(sum);
    }

    @Test
    public void testReflectGetParameters() throws NoSuchMethodException {
        Method method = WhileTest.class.getDeclaredMethod("lambda$sum1$0", int.class, JContext.class);
        Parameter[] parameters = method.getParameters();
        int parameterCount = method.getParameterCount();
        Assertions.assertEquals(parameterCount, parameters.length);
        method = WhileTest.class.getDeclaredMethod("lambda$sum1$1", Object[].class, JContext.class);
        parameters = method.getParameters();
        parameterCount = method.getParameterCount();
        Assertions.assertEquals(parameterCount, parameters.length);
    }

    @Async
    public JPromise<Integer> sum2(int to) throws InterruptedException {
        int sum = 0;
        while (sum < JPromise.just(to).await()) {
            sum += JPromise.just(1).await();
        }
        return JPromise.just(sum);
    }

    @Async
    public JPromise<Integer> multi1(int a, int b) throws InterruptedException {
        int res = 0;
        int i = 0, j = 0;
        while (i++ < a) {
            while (j++ < b) {
                res += JPromise.just(1).await();
            }
            j = 0;
        }
        return JPromise.just(res);
    }

    @Async
    public JPromise<Integer> multi2(int a, int b) throws InterruptedException {
        int res = 0;
        int i = 0, j = 0;
        while (i++ < JPromise.just(a).await()) {
            while (j++ < JPromise.just(b).await()) {
                res += JPromise.just(1).await();
            }
            j = 0;
        }
        return JPromise.just(res);
    }

    @Test
    public void test1() throws InterruptedException {
        Assertions.assertEquals(2000000, sum1(2000000).block());
    }

    @Test
    public void test2() throws InterruptedException {
        Assertions.assertEquals(100000, sum2(100000).block());
    }

    @Test
    public void test3() throws InterruptedException {
        Assertions.assertEquals(123 * 321, multi1(123, 321).block());
    }

    @Test
    public void test4() throws InterruptedException {
        Assertions.assertEquals(123 * 321, multi2(123, 321).block());
    }

    private static class TestObject {

        private int i = 10;

        public boolean ok() {
            return i >= 0;
        }

        public JPromise<TestObject> getObj() {
            return JPromise.just(this);
        }

        public JPromise<Void> consume() {
            --i;
            return JPromise.empty();
        }

        public JPromise<Void> consume(Object o) {
            if (o == this) {
                return consume();
            } else {
                return JPromise.empty();
            }
        }

        public JPromise<Void> close() {
            ++i;
            return JPromise.empty();
        }
    }

    private JPromise<Integer> whileWithFinally(TestObject n, Object o) throws InterruptedException {
        try {
            if (o != null) {
                n.consume(o).await();
            }
            while (n.ok()) {
                Object obj = n.getObj().await();
                n.consume(obj).await();
            }
        } finally {
            n.close().await();
        }
        return JPromise.just(n.i);
    }

    @Test
    public void testWhileWithFinally() throws InterruptedException {
        TestObject n = new TestObject();
        Assertions.assertEquals(0, whileWithFinally(n, null).block());
        n = new TestObject();
        Assertions.assertEquals(0, whileWithFinally(n, n).block());
    }

    private JPromise<Integer> onlyLoopInTryBlock(TestObject n) throws InterruptedException {
        try {
            while (n.ok()) {
                Object obj = n.getObj().await();
                n.consume(obj).await();
            }
        } finally {
            n.close().await();
        }
        return JPromise.just(n.i);
    }

    @Test
    public void testOnlyLoopInTryBlock() throws InterruptedException {
        Assertions.assertEquals(0, onlyLoopInTryBlock(new TestObject()).block());
    }

    private JPromise<Void> longLoop() throws InterruptedException {
        //noinspection InfiniteLoopStatement
        while (true) {
            JPromise.sleep(1, TimeUnit.SECONDS).await();
        }
    }

    @Test
    public void testCancelLoop() throws InterruptedException {
        JHandle<Void> handle = longLoop().async();
        Thread.sleep(1000);
        handle.cancel();
        Assertions.assertThrows(InterruptedException.class, handle::block);
        Assertions.assertTrue(handle.isCanceled());
    }
}
