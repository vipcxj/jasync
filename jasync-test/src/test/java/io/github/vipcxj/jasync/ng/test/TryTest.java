package io.github.vipcxj.jasync.ng.test;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class TryTest {

    static class TestClosable {

        private final int target;
        private int i;

        TestClosable(int target) {
            this.target = target;
        }

        public boolean isSuccess() {
            return i >= target;
        }

        public JPromise<Integer> add(int n) {
            i += n;
            return JPromise.just(i);
        }

        public JPromise<Integer> get() {
            return JPromise.just(i);
        }

        public JPromise<Void> close() {
            return JPromise.empty();
        }
    }

    @Async
    private JPromise<String> tryWithCatch1(JPromise<String> what) throws InterruptedException {
        String message = "hello";
        try {
            String await = what.await();
            if (await != null) {
                message += " " + await;
            } else {
                throw new NullPointerException();
            }
        } catch (NullPointerException t) {
            message += " nothing";
        }
        return JPromise.just(message);
    }

    @Test
    public void testTryWithCatch1() throws InterruptedException {
        Assertions.assertEquals("hello world", tryWithCatch1(JPromise.just("world")).block());
        Assertions.assertEquals("hello nothing", tryWithCatch1(JPromise.empty()).block());
    }

    @Async
    private JPromise<String> tryWithCatch2(String what) throws InterruptedException {
        String message = "hello";
        try {
            if (what != null) {
                message += " " + JPromise.just(what).await();
            } else {
                throw new NullPointerException();
            }
        } catch (NullPointerException t) {
            message += JPromise.just(" nothing").await();
        }
        return JPromise.just(message);
    }

    @Test
    public void testTryWithCatch2() throws InterruptedException {
        Assertions.assertEquals("hello world", tryWithCatch2("world").block());
        Assertions.assertEquals("hello nothing", tryWithCatch2(null).block());
    }

    private JPromise<Void> throwError(Throwable t) {
        return JPromise.error(t);
    }

    private JPromise<Boolean> tryWithCatch3() throws InterruptedException {
        boolean res;
        try {
            throwError(new IllegalStateException()).await();
            res = true;
        } catch (IllegalStateException e) {
            res = false;
        }
        return JPromise.just(res);
    }

    @Test
    public void testTryWithCatch3() throws InterruptedException {
        Assertions.assertFalse(tryWithCatch3().block());
    }

    private JPromise<Boolean> tryWithCatch4() throws InterruptedException {
        boolean res;
        try {
            throwError(new IllegalStateException()).await();
            res = JPromise.just(true).await();
        } catch (IllegalStateException e) {
            res = false;
        }
        return JPromise.just(res);
    }

    @Test
    public void testTryWithCatch4() throws InterruptedException {
        Assertions.assertFalse(tryWithCatch4().block());
    }

    private JPromise<Boolean> tryWithCatch5() throws InterruptedException {
        boolean res;
        try {
            throwError(new IllegalStateException()).await();
            res = true;
        } catch (IllegalStateException e) {
            res = false;
        }
        res = JPromise.just(res).await();
        return JPromise.just(res);
    }

    @Test
    public void testTryWithCatch5() throws InterruptedException {
        Assertions.assertFalse(tryWithCatch5().block());
    }

    private JPromise<Boolean> returnInTryWith1() {
        try {
            return throwError(new IllegalArgumentException()).thenReturn(true);
        } catch (IllegalArgumentException e) {
            return JPromise.just(false);
        }
    }

    @Test
    public void testReturnInTryWith() throws InterruptedException {
        Assertions.assertFalse(returnInTryWith1().block());
    }

    private JPromise<Integer> returnInTryWith2() {
        try {
            try {
                return throwError(new IllegalStateException()).thenReturn(0);
            } catch (IllegalArgumentException e0) {
                return JPromise.just(1);
            }
        } catch (IllegalStateException e1) {
            return JPromise.just(2);
        }
    }

    @Test
    public void testReturnInTryWith2() throws InterruptedException {
        Assertions.assertEquals(2, returnInTryWith2().block());
    }

    private JPromise<String> tryWithMultiTypeCatch(String what) throws InterruptedException {
        String message = "hello ";
        try {
            if (what == null) {
                throw new NullPointerException();
            } else if ("error".equals(what)) {
                throw new IllegalArgumentException();
            } else {
                message += JPromise.just(what).await();
            }
        } catch (NullPointerException | IllegalArgumentException e) {
            message += "null";
        }
        return JPromise.just(message);
    }

    @Test
    public void testTryWithMultiTypeCatch() throws InterruptedException {
        Assertions.assertEquals("hello world", tryWithMultiTypeCatch("world").block());
        Assertions.assertEquals("hello null", tryWithMultiTypeCatch(null).block());
        Assertions.assertEquals("hello null", tryWithMultiTypeCatch("error").block());
    }

    private JPromise<String> tryWithMultiCatch1(String what, int flag) throws InterruptedException {
        String message = "hello";
        try {
            if (what == null) {
                throw new NullPointerException();
            } else if (flag == 1) {
                throw new IllegalStateException();
            } else if (flag == 2) {
                throw new NoSuchFieldException();
            } else if (flag == 3) {
                throw new UnsupportedOperationException();
            } else if (flag == 4) {
                throw new Throwable();
            }
            return JPromise.just(message + " " + what);
        } catch (NullPointerException t) {
            message += " null";
        } catch (IllegalStateException t) {
            message += JPromise.just(" illegal state").await();
        } catch (NoSuchFieldException t) {
            message += JPromise.just(" no such field").await();
        } catch (UnsupportedOperationException t) {
            message += " unsupported operation";
        } catch (Throwable t) {
            message += JPromise.just(" throwable").await();
        }
        return JPromise.just(message);
    }

    @Test
    public void testTryWithMultiCatch1() throws InterruptedException {
        Assertions.assertEquals("hello world", tryWithMultiCatch1("world", 0).block());
        Assertions.assertEquals("hello null", tryWithMultiCatch1(null, 0).block());
        Assertions.assertEquals("hello illegal state", tryWithMultiCatch1("world", 1).block());
        Assertions.assertEquals("hello no such field", tryWithMultiCatch1("world", 2).block());
        Assertions.assertEquals("hello unsupported operation", tryWithMultiCatch1("world", 3).block());
        Assertions.assertEquals("hello throwable", tryWithMultiCatch1("world", 4).block());
        Assertions.assertEquals("hello world", tryWithMultiCatch1("world", 5).block());
    }

    @Async
    private JPromise<String> tryWithMultiCatch2() throws InterruptedException {
        String message = "";
        try {
            try {
                throw new NullPointerException();
            } catch (NullPointerException t) {
                message += JPromise.just("null").await();
                throw new IllegalArgumentException();
            } catch (IllegalArgumentException t) {
                message += " inner";
            }
        } catch (IllegalArgumentException t) {
            message += " outer";
        }
        return JPromise.just(message);
    }

    @Test
    public void testTryWithMultiCatch2() throws InterruptedException {
        Assertions.assertEquals("null outer", tryWithMultiCatch2().block());
    }

    private int tryFinallyCatchContinueNoAwait() {
        int a = 0;
        for (int i = 0; i < 10; ++i) {
            try {
                if (i % 2 == 0) {
                    continue;
                }
                ++a;
            } finally {
                ++a;
            }
        }
        return a;
    }

    @Async
    private JPromise<Integer> tryFinallyCatchContinue() throws InterruptedException {
        int a = 0;
        for (int i = 0; i < 10; ++i) {
            try {
                if (i % JPromise.just(2).await() == 0) {
                    continue;
                }
                ++a;
            } finally {
                ++a;
            }
        }
        return JPromise.just(a);
    }

    @Test
    public void testTryFinallyCatchContinue() throws InterruptedException {
        Assertions.assertEquals(tryFinallyCatchContinueNoAwait(), tryFinallyCatchContinue().block());
    }

    private int tryFinallyCatchBreakNoAwait() {
        int a = 0;
        for (int i = 0; i < 10; ++i) {
            try {
                if (i == 5) {
                    break;
                }
                ++a;
            } finally {
                ++a;
            }
        }
        return a;
    }

    @Async
    private JPromise<Integer> tryFinallyCatchBreak() throws InterruptedException {
        int a = 0;
        for (int i = 0; i < 10; ++i) {
            try {
                if (i == JPromise.just(5).await()) {
                    break;
                }
                ++a;
            } finally {
                ++a;
            }
        }
        return JPromise.just(a);
    }

    @Test
    public void testTryFinallyCatchBreak() throws InterruptedException {
        Assertions.assertEquals(tryFinallyCatchBreakNoAwait(), tryFinallyCatchBreak().block());
    }

    private int tryFinallyCatchReturnNoAwait() {
        int a = 0;
        for (int i = 0; i < 10; ++i) {
            try {
                if (i == 5) {
                    return a;
                }
                ++a;
            } finally {
                ++a;
            }
        }
        return a;
    }

    @Async
    private JPromise<Integer> tryFinallyCatchReturn() throws InterruptedException {
        int a = 0;
        for (int i = 0; i < 10; ++i) {
            try {
                if (i == JPromise.just(5).await()) {
                    return JPromise.just(a);
                }
                ++a;
            } finally {
                ++a;
            }
        }
        return JPromise.just(a);
    }

    @Test
    public void testTryFinallyCatchReturn() throws InterruptedException {
        Assertions.assertEquals(tryFinallyCatchReturnNoAwait(), tryFinallyCatchReturn().block());
    }

    private JPromise<Integer> tryFinallyWithLoop1(int target) throws InterruptedException {
        TestClosable closable = new TestClosable(target);
        try {
            for (int i = 0; i < 10; ++i) {
                closable.add(i).await();
                if (closable.isSuccess()) {
                    break;
                }
            }
            return closable.get();
        } finally {
            closable.close().await();
        }
    }

    @Test
    public void testTryFinallyWithLoop1() throws InterruptedException {
        Assertions.assertEquals(10, tryFinallyWithLoop1(10).block());
        Assertions.assertEquals(36, tryFinallyWithLoop1(30).block());
        Assertions.assertEquals(45, tryFinallyWithLoop1(45).block());
        Assertions.assertEquals(45, tryFinallyWithLoop1(50).block());
    }

    private JPromise<Void> doSomething() {
        return JPromise.empty();
    }

    private JPromise<Integer> tryFinallyWithLoop2(int target) throws InterruptedException {
        TestClosable closable = new TestClosable(target);
        try {
            for (int i = 0; i < 10; ++i) {
                // not use closable in the loop body
                doSomething().await();
            }
            // use closable out of the loop body
            return closable.get();
        } finally {
            closable.close().await();
        }
    }

    @Test
    public void testTryFinallyWithLoop2() throws InterruptedException {
        Assertions.assertEquals(0, tryFinallyWithLoop2(5).block());
        Assertions.assertEquals(0, tryFinallyWithLoop2(10).block());
    }

    static class TestContext {
        private final int target;
        private int i;

        TestContext(int target) {
            this.target = target;
        }

        TestBuf createBuff() {
            return new TestBuf();
        }
        JPromise<Void> readSomeBufUntilAny(TestBuf buf) {
            if (buf != null) {
                buf.add();
            }
            ++i;
            return JPromise.empty();
        }
        boolean isSuccess() {
            return i == target;
        }
    }

    static class TestBuf {
        private int i = 0;
        private void add() {
            ++i;
        }
        void release() {}
    }

    public JPromise<Integer> tryFinallyWithLoop3(TestContext context, boolean storeIfRead) throws InterruptedException {
        // Here local var byteBuf is merged by null and TestBuf. The index of it should also be merged.
        // Then calcLocalVars will take byteBuf into account.
        TestBuf byteBuf = storeIfRead ? context.createBuff() : null;
        try {
            while (!context.isSuccess()) {
                context.readSomeBufUntilAny(byteBuf).await();
            }
            return byteBuf != null ? JPromise.just(byteBuf.i) : JPromise.just(context.i);
        } finally {
            if (byteBuf != null)
                byteBuf.release();
        }
    }

    @Test
    public void testTryFinallyWithLoop3() throws InterruptedException {
        Assertions.assertEquals(5, tryFinallyWithLoop3(new TestContext(5), true).block());
        Assertions.assertEquals(5, tryFinallyWithLoop3(new TestContext(5), false).block());
    }

    private String tryFinallyCatchExceptionNoAwait() {
        String message = "hello";
        try {
            message += " world";
        } finally {
            message += ".";
        }
        return message;
    }

    @Async
    private JPromise<String> tryFinallyCatchException() throws InterruptedException {
        String message = "hello";
        try {
            message += JPromise.just(" world").await();
        } finally {
            message += JPromise.just(".").await();
        }
        return JPromise.just(message);
    }

    @Test
    public void testTryFinallyCatchException() throws InterruptedException {
        Assertions.assertEquals(tryFinallyCatchExceptionNoAwait(), tryFinallyCatchException().block());
    }

    private int tryCatchAndFinallyNoAwait(int flag) {
        int a = 0;
        try {
            try {
                switch (flag) {
                    case 0:
                        throw new NullPointerException();
                    case 1:
                        throw new IOException();
                    case 2:
                        throw new RuntimeException();
                    default:
                        a -= 1;
                }
            } catch (NullPointerException t) {
                a += 1;
            } catch (IOException t) {
                a += 2;
            } finally {
                a += 100;
            }
        } catch (Throwable ignored) {}
        return a;
    }

    @Async(logResultByteCode = Async.BYTE_CODE_OPTION_FULL_SUPPORT)
    private JPromise<Integer> tryCatchAndFinally(int flag) {
        int a = 0;
        try {
            try {
                switch (flag) {
                    case 0:
                        throw new NullPointerException();
                    case 1:
                        throw new IOException();
                    case 2:
                        throw new RuntimeException();
                    default:
                        a -= JPromise.just(1).await();
                }
            } catch (NullPointerException t) {
                a += JPromise.just(1).await();
            } catch (IOException t) {
                a += 2;
            } finally {
                a += JPromise.just(100).await();
            }
        } catch (Throwable ignored) {}
        return JPromise.just(a);
    }

    @Test
    public void testTryCatchAndFinally() throws InterruptedException {
        for (int i = 0; i < 5; ++i) {
            Assertions.assertEquals(tryCatchAndFinallyNoAwait(i), tryCatchAndFinally(i).block());
        }
    }

    private JPromise<Integer> nest0() {
        throw new RuntimeException();
    }

    private JPromise<Integer> nest1() throws InterruptedException {
        int out = nest0().await() + 1;
        return JPromise.just(out);
    }

    private JPromise<Integer> nest2() throws InterruptedException {
        int out = nest1().await() + 1;
        return JPromise.just(out);
    }

    private JPromise<Void> catchNestError() throws InterruptedException {
        RuntimeException exception = null;
        try {
            try {
                nest2().await();
            } catch (RuntimeException e) {
                exception = e;
                throw e;
            }
        } catch (RuntimeException e) {
            Assertions.assertSame(exception, e);
            StackTraceElement[] stackTrace = e.getStackTrace();
            Assertions.assertEquals(4, stackTrace.length);
            Assertions.assertEquals("nest0", stackTrace[0].getMethodName());
            Assertions.assertEquals("nest1", stackTrace[1].getMethodName());
            Assertions.assertEquals("nest2", stackTrace[2].getMethodName());
            Assertions.assertEquals("catchNestError", stackTrace[3].getMethodName());
            Assertions.assertEquals("io.github.vipcxj.jasync.ng.test.TryTest", stackTrace[0].getClassName());
            Assertions.assertEquals("io.github.vipcxj.jasync.ng.test.TryTest", stackTrace[1].getClassName());
            Assertions.assertEquals("io.github.vipcxj.jasync.ng.test.TryTest", stackTrace[2].getClassName());
            Assertions.assertEquals("io.github.vipcxj.jasync.ng.test.TryTest", stackTrace[3].getClassName());
            Assertions.assertEquals("TryTest.java", stackTrace[0].getFileName());
            Assertions.assertEquals("TryTest.java", stackTrace[1].getFileName());
            Assertions.assertEquals("TryTest.java", stackTrace[2].getFileName());
            Assertions.assertEquals("TryTest.java", stackTrace[3].getFileName());
            Assertions.assertEquals(543, stackTrace[0].getLineNumber());
            Assertions.assertEquals(547, stackTrace[1].getLineNumber());
            Assertions.assertEquals(552, stackTrace[2].getLineNumber());
            Assertions.assertEquals(560, stackTrace[3].getLineNumber());
            e.printStackTrace();
        }
        return JPromise.empty();
    }

    @Test
    public void testCatchNestError() throws InterruptedException {
        catchNestError().block();
    }
}