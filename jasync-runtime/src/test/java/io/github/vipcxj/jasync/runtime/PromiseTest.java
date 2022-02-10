package io.github.vipcxj.jasync.runtime;

import io.github.vipcxj.jasync.spec.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PromiseTest {

    @Test
    public void test() throws InterruptedException {
        for (int i = 0; i < 1; ++i) {
            JPromise2.sleep(1, TimeUnit.SECONDS)
                    .thenReturn(3)
                    .onSuccess(v -> {
                        System.out.println(Thread.currentThread().getName() + ": " + v);
                    })
                    .writeContext("a", 1)
                    .writeContext("b", 2)
                    .delay(1, TimeUnit.SECONDS)
                    .thenMap((Integer v) -> {
                        System.out.println("run here!");
                        return v + 1;
                    })
                    .onSuccess(v -> {
                        System.out.println(Thread.currentThread().getName() + ": " + v);
                    }).delay(1, TimeUnit.SECONDS)
                    .thenMap((Integer v) -> v + 1)
                    .onSuccess(v -> {
                        System.out.println(Thread.currentThread().getName() + ": " + v);
                    })
                    .then(() -> JAsync2.context().onSuccess(ctx -> {
                        System.out.println("a:" + ctx.get("a"));
                        System.out.println("b:" + ctx.get("b"));
                    }))
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
        Integer value = JPromise2.portal((JPortal<Integer> portal, JContext ignored) ->
                JAsync2.updateContext("index", 0, j -> j + 1)
                        .thenMap(ctx -> ctx.<Integer>get("index"))
                        .onSuccess(v -> {
                            if (v % 100000 == 0) {
                                System.out.println(Thread.currentThread().getName() + ": " + v);
                            }
                        })
                        .then(v -> {
                            if (v > 2000000) {
                                return JPromise2.just(v);
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

    private JPromise2<JContext> push(Object... args) {
        JPushContext pusher = JContext.createStackPusher();
        for (Object arg : args) {
            pusher.push(arg);
        }
        return pusher.complete();
    }

    @Test
    public void test4() throws InterruptedException {
        int i = 0;
        String msg = "a";
        JPromise2<String> task = push(msg, i).thenImmediate(() -> JPromise2.portal(factory -> JContext.popStack(stack -> {
            int i0 = (Integer) stack.pop();
            String msg0 = (String) stack.pop();
            long j = 0;
            if (i0 < 3) {
                return push(j, msg0, i0).thenImmediate(() -> JPromise2.<String>portal(factory1 -> JContext.popStack(stack1 -> {
                    int i1 = (Integer) stack1.pop();
                    String msg1 = (String) stack1.pop();
                    long j0 = (Long) stack1.pop();
                    if (j0 < 3) {
                        msg1 += "a";
                        ++j0;
                        return push(j0, msg1, i1).thenImmediate(factory1::jump);
                    } else {
                        ++i1;
                        return push(msg1, i1).thenImmediate(factory::jump);
                    }
                })));
            } else {
                return JPromise2.just(msg0);
            }
        })));
        String result = task.block();
        Assertions.assertEquals("aaaaaaaaaa", result);
    }

    @Test
    public void test5() throws InterruptedException {
        JPromise2<Integer> one = JPromise2.just(1);
        Integer two = one.then(v0 -> one.thenMap(v1 -> v0 + v1)).block();
        Assertions.assertEquals(2, two);
    }

    @Test
    public void test6() throws InterruptedException {
        Assertions.assertEquals(1, JPromise2.just(1).block());
    }

    @Test
    public void test7() {
        Assertions.assertThrows(InterruptedException.class, () -> {
            JPromise2<Integer> delay = JPromise2.just(1).delay(30, TimeUnit.SECONDS);
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
            JPromise2<Integer> one = JPromise2.just(1);
            JPromise2<Integer> delay = one.delay(30, TimeUnit.SECONDS);
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
}
