package io.github.vipcxj.jasync.ng.runtime;

import io.github.vipcxj.jasync.ng.spec.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PromiseTest {

    @Test
    public void test() throws InterruptedException {
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
    }

    @Test
    public void test1() throws InterruptedException {
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
    }

    @Test
    public void test2() throws InterruptedException {
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
    }

    @Test
    public void test3() throws InterruptedException {
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
    }

    @Test
    public void test4() throws InterruptedException {
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
    }

    @Test
    public void test5() throws InterruptedException {
        JPromise<Integer> one = JPromise.just(1);
        Integer two = one.then(v0 -> one.thenMap(v1 -> v0 + v1)).block();
        Assertions.assertEquals(2, two);
    }

    @Test
    public void test6() throws InterruptedException {
        Assertions.assertEquals(1, JPromise.just(1).block());
    }

    @Test
    public void test7() {
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
    }

    @Test
    public void test8() {
        Assertions.assertThrows(InterruptedException.class, () -> {
            JPromise<Integer> one = JPromise.just(1);
            JPromise<Integer> delay = one.delay(30, TimeUnit.SECONDS);
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    one.cancel();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
            delay.block();
        });
    }

    @Test
    public void testOrder() {
        JPromise<Void> sleep3 = JPromise.create((jThunk, context) -> {
            sleep(3000, jThunk, context);
            System.out.println("Sleep 3000");
        });
        JPromise<Void> sleep1 = JPromise.create((thunk, context) -> {
            sleep(1000, thunk, context);
            System.out.println("Sleep 1000");
        });
        sleep3.async();
        sleep1.async();
        try {
            sleep3.then(() -> sleep1).then(() -> {
                System.out.println("After sleep 1000");
                return JPromise.empty();
            }).block();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testTwoFinally() throws InterruptedException {
        AtomicInteger i = new AtomicInteger();
        JPromise.empty().doFinally(() -> {
            i.incrementAndGet();
            return JPromise.empty();
        }).doFinally(() -> {
            i.incrementAndGet();
            return JPromise.empty();
        }).block();
        Assertions.assertEquals(2, i.get());
    }

    @Test
    public void testCancelLoop() throws InterruptedException {
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
    }

    @Test
    void testMultiThread() throws InterruptedException {
        JPromiseTrigger<Integer> trigger = JPromise.createTrigger();
        List<Thread> threads = new ArrayList<>();
        for (int j = 0; j < 30; ++j) {
            Thread t = new Thread(() -> {
                try {
                    Integer r = trigger.getPromise().thenMap(i -> i + 1).block();
                    Assertions.assertEquals(r, 2);
                    r = trigger.getPromise().thenMap(i -> i + 2).block();
                    Assertions.assertEquals(r, 3);
                } catch (InterruptedException ignored) {}
            });
            t.start();
            threads.add(t);
        }
        new Thread(() -> {
            try {
                Thread.sleep(500);
                trigger.resolve(1);
            } catch (InterruptedException ignored) {}
        }).start();
        for (Thread thread : threads) {
            thread.join();
        }
    }

    @Test
    void testLoop0() throws InterruptedException {
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
    }

    @Test
    void testLoop1() throws InterruptedException {
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
    }

    @Test
    void testLoop2() throws InterruptedException {
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
    }

    private void sleep(long time, JThunk<Void> thunk, JContext context) {
        try {
            Thread.sleep(time);
            thunk.resolve(null, context);
        } catch (InterruptedException e) {
            thunk.reject(e, context);
        }
    }
}
