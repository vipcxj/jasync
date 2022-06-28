package io.github.vipcxj.jasync.ng.test;

import io.github.vipcxj.jasync.ng.spec.JHandle;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.exceptions.JAsyncCompositeException;
import io.github.vipcxj.jasync.ng.spec.exceptions.JAsyncWrapException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CompositePromiseTest {

    private List<Integer> input;

    @BeforeEach
    void setup() {
        input = IntStream.range(0, 100).boxed().collect(Collectors.toList());
    }

    private JPromise<List<Integer>> allSuccess(List<Integer> input) {
        Random random = new Random();
        return JPromise.all(input.stream().map(i -> {
            int dur = random.nextInt( 200) + 300;
            return JPromise.sleep(dur, TimeUnit.MILLISECONDS).thenReturn(i);
        }).collect(Collectors.toList()));
    }

    @Test
    public void testAllSuccess() throws InterruptedException {
        Assertions.assertEquals(input, allSuccess(input).block());
    }

    private JPromise<List<Integer>> allError(List<Integer> input, Throwable error, int delay) {
        Random random = new Random();
        List<JPromise<? extends Integer>> promises = input.stream().map(i -> {
            int dur = random.nextInt(600) + 300;
            return JPromise.sleep(dur, TimeUnit.MILLISECONDS).thenReturn(i);
        }).collect(Collectors.toList());
        int pos = random.nextInt(promises.size());
        promises.add(pos, JPromise.sleep(delay, TimeUnit.MILLISECONDS).thenPromise(JPromise.error(error)));
        return JPromise.all(promises);
    }

    @Test
    public void testAllError() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            try {
                allError(input, new IllegalStateException(), 100).block();
            } catch (JAsyncWrapException e) {
                throw e.getCause();
            }
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            try {
                allError(input, new IllegalArgumentException(), 600).block();
            } catch (JAsyncWrapException e) {
                throw e.getCause();
            }
        });
        Assertions.assertThrows(IOException.class, () -> {
            try {
                allError(input, new IOException(), 1000).block();
            } catch (JAsyncWrapException e) {
                throw e.getCause();
            }
        });
    }

    @Test
    public void testAllCancel() {
        JPromise<List<Integer>> promise1 = allSuccess(input);
        JHandle<List<Integer>> handle = promise1.async();
        JPromise.sleep(100, TimeUnit.MILLISECONDS)
                .onFinally(handle::cancel).async();
        Assertions.assertThrows(InterruptedException.class, handle::block);
        Assertions.assertTrue(handle.isCompleted());
        Assertions.assertTrue(handle.isRejected());
        Assertions.assertTrue(handle.isCanceled());
        JPromise<List<Integer>> promise2 = allError(input, new IOException(), 300);
        handle = promise2.async();
        JPromise.sleep(100, TimeUnit.MILLISECONDS)
                .onFinally(handle::cancel).async();
        Assertions.assertThrows(InterruptedException.class, handle::block);
        Assertions.assertTrue(handle.isCompleted());
        Assertions.assertTrue(handle.isRejected());
        Assertions.assertTrue(handle.isCanceled());
    }

    private JPromise<Integer> anySuccess(List<Integer> input, int expect) {
        return JPromise.any(input.stream().map(i -> {
            int dur = i == expect ? 200 : 500;
            return JPromise.sleep(dur, TimeUnit.MILLISECONDS).thenReturn(i);
        }).collect(Collectors.toList()));
    }

    @Test
    public void testAnySuccess() throws InterruptedException {
        Assertions.assertEquals(5, anySuccess(input, 5).block());
        Assertions.assertEquals(26, anySuccess(input, 26).block());
        Assertions.assertEquals(88, anySuccess(input, 88).block());
    }

    private JPromise<Integer> anyError(List<Integer> input, Throwable error, int num, Throwable spError, int index) {
        Random random = new Random();
        return JPromise.any(input.stream().map(i -> {
            int dur = random.nextInt(300) + 300;
            return JPromise.sleep(dur, TimeUnit.MILLISECONDS).then(() -> {
                if (i == index) {
                    return JPromise.error(spError);
                } else if (i < num) {
                    return JPromise.error(error);
                } else {
                    return JPromise.just(i);
                }
            });
        }).collect(Collectors.toList()));
    }

    @Test
    public void testAnyError() throws InterruptedException {
        RuntimeException normError = new RuntimeException();
        IllegalStateException specialError = new IllegalStateException();
        Integer res = anyError(input, normError, 50, specialError, 13).block();
        Assertions.assertTrue(res >= 50);
        res = anyError(input, normError, 73, specialError, 13).block();
        Assertions.assertTrue(res >= 73);
        res = anyError(input, normError, 95, specialError, 13).block();
        Assertions.assertTrue(res >= 95);
        res = anyError(input, normError, 99, specialError, 13).block();
        Assertions.assertEquals(99, res);
        Assertions.assertThrows(JAsyncCompositeException.class, () -> {
            try {
                anyError(input, normError, 100, specialError, 13).block();
            } catch (JAsyncWrapException e) {
                Throwable cause = e.getCause();
                Assertions.assertTrue(cause instanceof JAsyncCompositeException);
                List<Throwable> errors = ((JAsyncCompositeException) cause).getErrors();
                int i = 0;
                for (Throwable error : errors) {
                    if (i++ == 13) {
                        Assertions.assertEquals(specialError, error);
                    } else {
                        Assertions.assertEquals(normError, error);
                    }
                }
                throw cause;
            }
        });
        Assertions.assertThrows(JAsyncCompositeException.class, () -> {
            try {
                anyError(input, normError, 101, specialError, 35).block();
            } catch (JAsyncWrapException e) {
                Throwable cause = e.getCause();
                Assertions.assertTrue(cause instanceof JAsyncCompositeException);
                List<Throwable> errors = ((JAsyncCompositeException) cause).getErrors();
                int i = 0;
                for (Throwable error : errors) {
                    if (i++ == 35) {
                        Assertions.assertEquals(specialError, error);
                    } else {
                        Assertions.assertEquals(normError, error);
                    }
                }
                throw cause;
            }
        });
    }

    @Test
    public void testAnyCancel() {
        JPromise<Integer> promise1 = anySuccess(input, 56);
        JHandle<Integer> handle = promise1.async();
        JPromise.sleep(100, TimeUnit.MILLISECONDS)
                .onFinally(handle::cancel).async();
        Assertions.assertThrows(InterruptedException.class, handle::block);
        Assertions.assertTrue(handle.isCompleted());
        Assertions.assertTrue(handle.isRejected());
        Assertions.assertTrue(handle.isCanceled());
        JPromise<Integer> promise2 = anyError(input, new RuntimeException(), 100, new IOException(), 33);
        handle = promise2.async();
        JPromise.sleep(100, TimeUnit.MILLISECONDS)
                .onFinally(handle::cancel).async();
        Assertions.assertThrows(InterruptedException.class, handle::block);
        Assertions.assertTrue(handle.isCompleted());
        Assertions.assertTrue(handle.isRejected());
        Assertions.assertTrue(handle.isCanceled());
    }

    private JPromise<Integer> raceSuccess(List<Integer> input, int expect) {
        return JPromise.race(input.stream().map(i -> {
            int dur = i == expect ? 200 : 500;
            return JPromise.sleep(dur, TimeUnit.MILLISECONDS).thenReturn(i);
        }).collect(Collectors.toList()));
    }

    @Test
    public void testRaceSuccess() throws InterruptedException {
        Assertions.assertEquals(5, raceSuccess(input, 5).block());
        Assertions.assertEquals(26, raceSuccess(input, 26).block());
        Assertions.assertEquals(88, raceSuccess(input, 88).block());
    }

    private JPromise<Integer> raceError(List<Integer> input, Throwable error, int delay) {
        Random random = new Random();
        List<JPromise<? extends Integer>> promises = input.stream().map(i -> {
            int dur = random.nextInt(200) + 300;
            return JPromise.sleep(dur, TimeUnit.MILLISECONDS).thenReturn(i);
        }).collect(Collectors.toList());
        int pos = random.nextInt(promises.size());
        promises.add(pos, JPromise.sleep(delay, TimeUnit.MILLISECONDS).thenPromise(JPromise.error(error)));
        return JPromise.race(promises);
    }

    @Test
    public void testRaceError() {
        Assertions.assertDoesNotThrow(() -> raceError(input, new RuntimeException(), 900));
        Assertions.assertThrows(IOException.class, () -> {
            try {
                raceError(input, new IOException(), 100).block();
            } catch (JAsyncWrapException e) {
                throw e.getCause();
            }
        });
    }

    @Test
    public void testRaceCancel() {
        JPromise<Integer> promise1 = raceSuccess(input, 56);
        JHandle<Integer> handle = promise1.async();
        JPromise.sleep(100, TimeUnit.MILLISECONDS)
                .onFinally(handle::cancel).async();
        Assertions.assertThrows(InterruptedException.class, handle::block);
        Assertions.assertTrue(handle.isCompleted());
        Assertions.assertTrue(handle.isRejected());
        Assertions.assertTrue(handle.isCanceled());
        JPromise<Integer> promise2 = raceError(input, new RuntimeException(), 300);
        handle = promise2.async();
        JPromise.sleep(100, TimeUnit.MILLISECONDS)
                .onFinally(handle::cancel).async();
        Assertions.assertThrows(InterruptedException.class, handle::block);
        Assertions.assertTrue(handle.isCompleted());
        Assertions.assertTrue(handle.isRejected());
        Assertions.assertTrue(handle.isCanceled());
    }
}
