package io.github.vipcxj.jasync.reactive;

import io.github.vipcxj.jasync.runtime.helpers.IntReference;
import io.github.vipcxj.jasync.runtime.helpers.ObjectReference;
import io.github.vipcxj.jasync.spec.JAsync;
import io.github.vipcxj.jasync.spec.JPromise2;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public class ReactorPromiseTest {

    @Test
    public void test1() {
        Promises.from(Mono.just(6).delayElement(Duration.ofSeconds(1))).then(a ->
                Promises.from(Mono.just(8).delayElement(Duration.ofSeconds(1))).then(b ->
                        JAsync.deferVoid(() -> {
                            IntReference iRef = new IntReference(0);
                            return JAsync.just().doWhileVoid(
                                    () -> iRef.getValue() < 10,
                                    () -> JAsync.deferVoid(() -> {
                                        if (iRef.getValue() % 2 == 0) {
                                            JAsync.doContinue(null);
                                        }
                                        System.out.println(iRef.getValue());
                                        return JAsync.just();
                                    }).doFinally(() -> {
                                        iRef.incrementAndGetValue();
                                        return JAsync.just();
                                    })
                            );
                        }).then(() ->
                                JAsync.deferVoid(() -> {
                                    AtomicInteger iRef = new AtomicInteger(0);
                                    return JAsync.just().doWhileVoid(
                                            () -> iRef.get() < a,
                                            () -> JAsync.defer(() -> {
                                                AtomicInteger jRef = new AtomicInteger(0);
                                                return JAsync.just().doWhileVoid(
                                                        () -> jRef.get() < b,
                                                        () -> JAsync.defer(() -> Promises.from(Mono.just(iRef.get() * jRef.get()).delayElement(Duration.ofSeconds(1))).<Void>then(t0 -> {
                                                            System.out.println(iRef.get() + " * " + jRef.get() + " = " + t0);
                                                            if (iRef.get() == 5) {
                                                                JAsync.doBreak(null);
                                                            } else if (iRef.get() == 3 && jRef.get() == 4) {
                                                                return JAsync.doReturn(null);
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
        ).catchReturn().block();
    }

    @Test
    public void test2() {
        int a = 0;
        try {
            a = 1;
        } catch (IllegalAccessError e1) {
            e1.printStackTrace();
        } catch (UnknownError e2) {
            e2.printStackTrace();
        } catch (Exception e3) {
            e3.printStackTrace();
        } finally {
            a += 1;
        }
    }
}
