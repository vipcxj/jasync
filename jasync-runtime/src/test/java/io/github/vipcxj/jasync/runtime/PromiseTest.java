package io.github.vipcxj.jasync.runtime;

import io.github.vipcxj.jasync.spec.JAsync2;
import io.github.vipcxj.jasync.spec.JHandle;
import io.github.vipcxj.jasync.spec.JPortal;
import io.github.vipcxj.jasync.spec.JPromise2;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

public class PromiseTest {

    @Test
    public void test() {
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
    public void test1() {
        Integer value = JPromise2.portal((JPortal<Integer> portal) ->
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
}
