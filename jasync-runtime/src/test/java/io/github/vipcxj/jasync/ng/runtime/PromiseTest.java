package io.github.vipcxj.jasync.ng.runtime;

import io.github.vipcxj.jasync.ng.spec.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class PromiseTest {

    @Test
    void test0() throws InterruptedException {
        System.out.println("test0 starting");
        JPromise.just(1).thenMap(v -> v + 1).thenMap(v -> v + 1).block();
        System.out.println("test0 completed");
    }

    @Test
    void test() throws InterruptedException {
        System.out.println("test starting");
        for (int i = 0; i < 1; ++i) {
            JPromise.sleep(1, TimeUnit.SECONDS)
                    .thenReturn(3)
                    .onSuccess(v -> System.out.println(Thread.currentThread().getName() + ": " + v))
                    .withSetContextValue("a", 1)
                    .withSetContextValue("b", 2)
                    .delay(1, TimeUnit.SECONDS)
                    .thenMap((Integer v) -> {
                        System.out.println("run here!");
                        return v + 1;
                    })
                    .onSuccess(v -> System.out.println(Thread.currentThread().getName() + ": " + v)).delay(1, TimeUnit.SECONDS)
                    .thenMap((Integer v) -> v + 1)
                    .onSuccess(v -> {
                        System.out.println(Thread.currentThread().getName() + ": " + v);
                    })
                    .withContext(ctx -> {
                        System.out.println("a:" + ctx.get("a"));
                        System.out.println("b:" + ctx.get("b"));
                    })
                    .block();
        }
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("test completed");
    }

    @Test
    void test1() throws InterruptedException {
        System.out.println("test1 starting");
        Integer value = JPromise.portal((JPortal<Integer> portal, JContext ignored) ->
                JPromise.updateContextValue("index", j -> j + 1, 0)
                        .thenMapWithContextImmediate(ctx -> ctx.<Integer>get("index"))
                        .onSuccess(v -> {
                            if (v % 100000 == 0) {
                                System.out.println(Thread.currentThread().getName() + ": " + v);
                            }
                        })
                        .then(v -> {
                            if (v > 2000000) {
                                return JPromise.just(v);
                            } else {
                                return portal.jump();
                            }
                        })
                        .thenMap(v -> {
                            System.out.println(v);
                            return v + 1;
                        }))
                .onSuccess(v -> System.out.println("ok"))
                .block();
        System.out.println(value);
        System.out.println("test1 completed");
    }

    @Test
    void test2() throws InterruptedException {
        System.out.println("test2 starting");
        ScheduledExecutorService service = Executors.newScheduledThreadPool(4);
        for (int i = 0; i < 8; ++i) {
            service.submit(() -> {
                try {
                    System.out.println(Thread.currentThread().getName() + ": task start");
                    Thread.sleep(3000);
                    System.out.println(Thread.currentThread().getName() + ": task end");
                    long current = System.currentTimeMillis();
                    service.schedule(() -> {
                        System.out.println(Thread.currentThread().getName() + ": run scheduled task here after " + (System.currentTimeMillis() - current));
                    }, 1, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {}
            });
        }
        Thread.sleep(10000);
        System.out.println("test2 completed");
    }

    @Test
    void test3() throws InterruptedException {
        System.out.println("test3 starting");
        ExecutorService service = Executors.newWorkStealingPool(4);
        for (int i = 0; i < 8; ++i) {
            service.submit(() -> {
                try {
                    System.out.println(Thread.currentThread().getName() + ": task start");
                    Thread.sleep(3000);
                    System.out.println(Thread.currentThread().getName() + ": task end");
                    long current = System.currentTimeMillis();
                    new Thread(() -> {
                        try {
                            Thread.sleep(1000);
                            service.submit(() -> {
                                System.out.println(Thread.currentThread().getName() + ": run scheduled task here after " + (System.currentTimeMillis() - current));
                            });
                        } catch (InterruptedException ignored) {}
                    }).start();
                } catch (InterruptedException ignored) {}
            });
        }
        Thread.sleep(10000);
        System.out.println("test3 completed");
    }

    @Test
    void test4() throws InterruptedException {
        System.out.println("test4 starting");
        int i = 0;
        String msg = "a";
        JPromise<String> task = JPromise.portal(locals0 -> {
            String msg0 = (String) locals0[0];
            int i0 = (Integer) locals0[1];
            long j = 0;
            if (i0 < 3) {
                return JPromise.portal(locals1 -> {
                    long j0 = (Long) locals1[0];
                    String msg1 = (String) locals1[1];
                    int i1 = (Integer) locals1[2];
                    if (j0 < 3) {
                        msg1 += "a";
                        ++j0;
                        return JPromise.jump(1, j0, msg1, i1);
                    } else {
                        ++i1;
                        return JPromise.jump(0, msg1, i1);
                    }
                }, 1, j, msg0, i0);
            } else {
                return JPromise.just(msg0);
            }
        }, 0, msg, i);
        String result = task.block();
        Assertions.assertEquals("aaaaaaaaaa", result);
        System.out.println("test4 completed");
    }

    @Test
    void test5() throws InterruptedException {
        System.out.println("test5 starting");
        JPromise<Integer> one = JPromise.just(1);
        Integer two = one.then(v0 -> one.thenMap(v1 -> v0 + v1)).block();
        Assertions.assertEquals(2, two);
        System.out.println("test5 completed");
    }

    @Test
    void test6() throws InterruptedException {
        System.out.println("test6 starting");
        Assertions.assertEquals(1, JPromise.just(1).block());
        System.out.println("test6 completed");
    }

    @Test
    void test7() {
        System.out.println("test7 starting");
        Assertions.assertThrows(InterruptedException.class, () -> {
            JPromise<Integer> delay = JPromise.just(1).delay(30, TimeUnit.SECONDS);
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    delay.cancel();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
            delay.block();
        });
        System.out.println("test7 completed");
    }

    @Test
    void test8() {
        Assertions.assertThrows(InterruptedException.class, () -> {
            System.out.println("test8 starting");
            JPromise<Integer> one = JPromise.just(1);
            JPromise<Integer> delay = one.delay(30, TimeUnit.SECONDS);
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    delay.cancel();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
            delay.block();
        });
        System.out.println("test8 completed");
    }

    @Test
    void testOrder() {
        System.out.println("testOrder starting");
        JPromise<Void> sleep3 = JPromise.create((jThunk, context) -> {
            sleep(3000, jThunk, context);
            System.out.println("Sleep 3000");
        });
        JPromise<Void> sleep1 = JPromise.create((thunk, context) -> {
            sleep(1000, thunk, context);
            System.out.println("Sleep 1000");
        });
        JHandle<Object> handle3 = sleep3.then(() -> sleep1).then(() -> {
            System.out.println("After sleep 1000");
            return JPromise.empty();
        }).async();
        JHandle<Void> handle1 = sleep1.async();
        try {
            handle3.block();
            handle1.block();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("testOrder completed");
    }

    @Test
    void testTwoFinally() throws InterruptedException {
        System.out.println("testTwoFinally starting");
        AtomicInteger i = new AtomicInteger();
        JPromise.empty().doFinally(() -> {
            i.incrementAndGet();
            return JPromise.empty();
        }).doFinally(() -> {
            i.incrementAndGet();
            return JPromise.empty();
        }).block();
        Assertions.assertEquals(2, i.get());
        System.out.println("testTwoFinally completed");
    }

    @Test
    void testCancelLoop() throws InterruptedException {
        System.out.println("testCancelLoop starting");
        JPromise<Object> loop = JPromise.portal(portal -> {
            return JPromise.sleep(1, TimeUnit.SECONDS)
                    .onFinally(() -> System.out.println("ping"))
                    .then(portal::jump);
        });
        loop.async();
        JPromise.race(
                JPromise.sleep(3010, TimeUnit.MILLISECONDS).onFinally(loop::cancel),
                loop
        ).block();
        System.out.println("testCancelLoop completed");
    }

    @Test
    void testMultiThread() throws InterruptedException {
        System.out.println("testMultiThread starting");
        JPromiseTrigger<Integer> trigger = JPromise.createTrigger();
        List<Thread> threads = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger(0);
        for (int j = 0; j < 60; ++j) {
            Thread t = new Thread(() -> {
                try {
                    Thread.sleep(1);
                    Integer r = trigger.getPromise().thenMapImmediate(i -> i + 1).block();
                    Assertions.assertEquals(r, 2);
                    r = trigger.getPromise().thenMapImmediate(i -> i + 2).block();
                    Assertions.assertEquals(r, 3);
                    counter.incrementAndGet();
                } catch (InterruptedException ignored) {}
            });
            t.start();
            threads.add(t);
        }
        Thread t =new Thread(() -> {
            try {
                Thread.sleep(15);
                trigger.resolve(1);
            } catch (InterruptedException ignored) {}
        });
        t.start();
        for (Thread thread : threads) {
            thread.join();
        }
        t.join();
        Assertions.assertEquals(60, counter.get());
        System.out.println("testMultiThread completed");
    }

    @Test
    void testLoop0() throws InterruptedException {
        System.out.println("testLoop0 starting");
        long res = JPromise.portal(locals -> {
            String var0 = (String) locals[0];
            int var1 = (Integer) locals[1];
            long var2 = (Long) locals[2];
            if (var1 < 10) {
                var2 += var1;
                ++var1;
                return JPromise.jump(0, var0, var1, var2);
            } else {
                return JPromise.just(var2);
            }
        }, 0, "a", 0, 0L).block();
        Assertions.assertEquals(45, res);
        System.out.println("testLoop0 completed");
    }

    @Test
    void testLoop1() throws InterruptedException {
        System.out.println("testLoop1 starting");
        long res = JPromise.portal(locals -> {
            String var0 = (String) locals[0];
            int i = (Integer) locals[1];
            int j = (Integer) locals[2];
            long sum = (Long) locals[3];
            if (i < 10) {
                return JPromise.portal(locals1 -> {
                    String var00 = (String) locals1[0];
                    int i0 = (Integer) locals1[1];
                    int j0 = (Integer) locals1[2];
                    long sum0 = (Long) locals1[3];
                    if (j0 < 10) {
                        sum0 += 1;
                        ++j0;
                        return JPromise.jump(1, var00, i0, j0, sum0);
                    } else {
                        ++i0;
                        j0 = 0;
                        return JPromise.jump(0, var00, i0, j0, sum0);
                    }
                }, 1, var0, i, j, sum);
            } else {
                return JPromise.just(sum);
            }
        }, 0, "a", 0, 0, 0L).block();
        Assertions.assertEquals(100, res);
        System.out.println("testLoop1 completed");
    }

    @Test
    void testLoop2() throws InterruptedException {
        System.out.println("testLoop2 starting");
        JPromiseTrigger<Integer> trigger = JPromise.createTrigger();
        trigger.resolve(10);
        Integer res = JPromise.portal(locals -> {
            final int i = (Integer) locals[0];
            return JPromise.wrap(context -> {
                return JPromise.wrapContext(trigger.getPromise(), context).thenWithContext((n, ctx) -> {
                    if (i < n) {
                        return JPromise.jump(0, i + 1);
                    } else {
                        return JPromise.just(i);
                    }
                });
            });
        }, 0, 0).block();
        Assertions.assertEquals(10, res);
        System.out.println("testLoop2 completed");
    }

    private void sleep(long time, JThunk<Void> thunk, JContext context) {
        try {
            Thread.sleep(time);
            thunk.resolve(null, context);
        } catch (InterruptedException e) {
            thunk.reject(e, context);
        }
    }

    JPromise<Void> task(JAsyncReadWriteLock lock, AtomicInteger iter, int target) {
        return lock.readLock().lock().thenWithContext((ctx1) -> {
            System.out.println("[" + Thread.currentThread().getName() + "](ctx " + ctx1.id() + "): After shared locked, shared lock count " + ctx1.getSharedLockCount());
            iter.incrementAndGet();
            return JPromise.sleep(2, TimeUnit.SECONDS).thenWithContext((ctx2) -> {
                Assertions.assertEquals(target, iter.get());
                Assertions.assertFalse(lock.writeLock().tryLock(ctx2));
                lock.readLock().unlock(ctx2);
                System.out.println("[" + Thread.currentThread().getName() + "](ctx " + ctx2.id() + "): After shared unlocked, shared lock count " + ctx2.getSharedLockCount());
                return JPromise.empty();
            });
        });
    }

    @Test
    void testReadLock() throws InterruptedException {
        System.out.println("testReadLock starting");
        JAsyncReadWriteLock lock = JPromise.readWriteLock();
        AtomicInteger iter = new AtomicInteger();
        List<JHandle<Void>> handles = new ArrayList<>();
        for (int i = 0; i < 10; ++i) {
            JHandle<Void> handle = task(lock, iter, 10).async();
            handles.add(handle);
        }
        for (JHandle<Void> handle : handles) {
            handle.block();
        }
        System.out.println("testReadLock completed");
    }

    @Test
    void testWriteLock() throws InterruptedException {
        System.out.println("testWriteLock starting");
        JAsyncReadWriteLock lock = JPromise.readWriteLock();
        AtomicInteger iter = new AtomicInteger();
        List<JHandle<Void>> handles = new ArrayList<>();
        for (int i = 0; i < 5; ++i) {
            JHandle<Void> handle = task(lock, iter, 5).async();
            handles.add(handle);
        }
        JPromise.sleep(500, TimeUnit.MILLISECONDS).thenWithContextImmediate((ctx1) -> {
            return lock.writeLock().lock().then(() -> {
                System.out.println("[" + Thread.currentThread().getName() + "](ctx " + ctx1.id() + "): After ex locked, shared lock count " + ctx1.getSharedLockCount());
                int i = iter.incrementAndGet();
                return JPromise.sleep(1, TimeUnit.SECONDS).thenWithContext(ctx2 -> {
                    Assertions.assertEquals(i, iter.get());
                    lock.writeLock().unlock(ctx2);
                    System.out.println("[" + Thread.currentThread().getName() + "](ctx " + ctx2.id() + "): After ex unlocked, shared lock count " + ctx2.getSharedLockCount());
                    return JPromise.empty();
                });
            });
        }).block();
        for (JHandle<Void> handle : handles) {
            Assertions.assertTrue(handle.isResolved());
        }
        System.out.println("testWriteLock completed");
    }

    @Test
    void testReadWriteLock() throws InterruptedException {
        Thread thread1 = new Thread(() -> {
            try {
                testWriteLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread1.start();
        Thread thread2 = new Thread(() -> {
            try {
                Thread.sleep(500);
                testReadLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread2.start();
        thread1.join();
        thread2.join();
    }

    @Test
    void testLock() throws InterruptedException {
        System.out.println("testLock starting");
        JAsyncReadWriteLock lock = JPromise.readWriteLock();
        List<JHandle<Void>> handles = new ArrayList<>();
        long[] res = new long[1];
        for (int i = 0; i < 10000; ++i) {
            JPromise<Void> promise = lock.writeLock().lock().thenWithContextImmediate((ctx) -> {
                for (int j = 0; j < 100000; ++j) {
                    ++res[0];
                }
                lock.writeLock().unlock(ctx);
                return JPromise.empty();
            });
            handles.add(promise.async());
        }
        for (JHandle<Void> handle : handles) {
            handle.block();
        }
        Assertions.assertEquals(10000L * 100000L, res[0]);
        for (int i = 0; i < 10000; ++i) {
            JPromise<Void> promise = JPromise.empty().then(() -> {
                for (int j = 0; j < 100000L; ++j) {
                    ++res[0];
                }
                return JPromise.empty();
            });
            handles.add(promise.async());
        }
        for (JHandle<Void> handle : handles) {
            handle.block();
        }
        System.out.println(res[0]);
        System.out.println("testLock completed");
    }

    @Test
    void testCompletableFutureThatHasCompleted() throws InterruptedException {
        CompletableFuture<String> stage = new CompletableFuture<>();
        stage.complete("test");
        Assertions.assertEquals("test", JPromise.from(stage).block());
    }

    @Test
    void testCompletableFutureThatNotCompleted() throws InterruptedException {
        CompletableFuture<String> stage = new CompletableFuture<>();
        JHandle<String> handle = JPromise.from(stage).async();
        stage.complete("test");
        Assertions.assertEquals("test", handle.block());
    }
}
