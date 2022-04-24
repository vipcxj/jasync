package io.github.vipcxj.jasync.ng.test;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class TryTest {

    @Async
    private JPromise<String> tryWithCatch1(JPromise<String> what) {
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
    private JPromise<String> tryWithCatch2(String what) {
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

    private JPromise<Boolean> tryWithCatch3() {
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

    private JPromise<Boolean> tryWithCatch4() {
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

    private JPromise<Boolean> tryWithCatch5() {
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

    private JPromise<String> tryWithMultiTypeCatch(String what) {
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

    private JPromise<String> tryWithMultiCatch1(String what, int flag) {
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
    private JPromise<String> tryWithMultiCatch2() {
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
    private JPromise<Integer> tryFinallyCatchContinue() {
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
    private JPromise<Integer> tryFinallyCatchBreak() {
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
    private JPromise<Integer> tryFinallyCatchReturn() {
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
    private JPromise<String> tryFinallyCatchException() {
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

    @Async
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
}