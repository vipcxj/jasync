package io.github.vipcxj.jasync.reactive;

import io.github.vipcxj.jasync.Promise;
import io.github.vipcxj.jasync.annotations.Async;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public class MonoPromiseTest {

    private boolean always() {
        return true;
    }

    private void test1_() {
        @Async Integer a = Promises.from(Mono.just(6).delayElement(Duration.ofSeconds(1))).await();
        Integer b = Promises.from(Mono.just(8).delayElement(Duration.ofSeconds(1))).await();
        for (int i = 0; i < a; ++i) {
            for (int j = 0; j < b; ++j) {
                System.out.println(Promises.from(Mono.just(i * j).delayElement(Duration.ofSeconds(1))).await());
                if (i == 5) {
                    break;
                } else if (i == 3 && j == 4) {
                    return;
                }
            }
        }
    }

    @Test
    public void test1() {
        Promises.from(Mono.just(6).delayElement(Duration.ofSeconds(1))).then(a ->
                Promises.from(Mono.just(8).delayElement(Duration.ofSeconds(1))).then(b ->
                        Promise.defer(() -> {
                            AtomicInteger iRef = new AtomicInteger(0);
                            return Promise.just().doWhileVoid(
                                    () -> iRef.get() < 10,
                                    () -> Promise.defer(() -> {
                                        if (iRef.get() % 2 == 0) {
                                            Promise.doContinue();
                                        }
                                        System.out.println(iRef.get());
                                        return Promise.just();
                                    }).doFinally(() -> {
                                        iRef.set(iRef.get() + 1);
                                        return Promise.just();
                                    })
                            );
                        }).then(() ->
                                Promise.defer(() -> {
                                    AtomicInteger iRef = new AtomicInteger(0);
                                    return Promise.just().doWhileVoid(
                                            () -> iRef.get() < a,
                                            () -> Promise.defer(() -> {
                                                AtomicInteger jRef = new AtomicInteger(0);
                                                return Promise.just().doWhileVoid(
                                                        () -> jRef.get() < b,
                                                        () -> Promise.defer(() -> Promises.from(Mono.just(iRef.get() * jRef.get()).delayElement(Duration.ofSeconds(1))).<Void>then(t0 -> {
                                                            System.out.println(iRef.get() + " * " + jRef.get() + " = " + t0);
                                                            if (iRef.get() == 5) {
                                                                Promise.doBreak();
                                                            } else if (iRef.get() == 3 && jRef.get() == 4) {
                                                                return Promise.doReturn(null);
                                                            }
                                                            return null;
                                                        })).doFinally(() -> {
                                                            jRef.set(jRef.get() + 1);
                                                            return null;
                                                        })
                                                );
                                            }).doFinally(() -> {
                                                iRef.set(iRef.get() + 1);
                                                return null;
                                            })
                                    );
                                })
                        )
                )
        ).doReturn().block();
    }
}
