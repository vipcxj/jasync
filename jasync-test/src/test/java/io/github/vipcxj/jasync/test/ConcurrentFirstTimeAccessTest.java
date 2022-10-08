package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.reactive.Promises;
import io.github.vipcxj.jasync.spec.JPromise;
import io.github.vipcxj.jasync.spec.annotations.Async;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConcurrentFirstTimeAccessTest {

    @Test
    public void testAsyncConcurrentFirstTimeAccess() {
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 10; ++i) {
            final int index = i;
            Thread t = new Thread(() -> {
                System.out.println("[" + index + "] Thread start");
                Boolean success = false;
                try {
                    Mono<Boolean> result = doSomething().unwrap(Mono.class);
                    success = result.block();
                    System.out.println("[" + index + "] Success");
                } catch (Exception e) {
                    System.out.println("[" + index + "] Error");
                    e.printStackTrace();
                }
                assertEquals(Boolean.TRUE, success);
            });
            threads.add(t);
        }
        for (Thread thread : threads) {
            thread.start();
        }
        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (Exception ignored) {}
    }

    @Async
    private JPromise<Boolean> doSomething(){
        Promises.from(Mono.delay(Duration.ofMillis(100L))).await();

        return Promises.from(Mono.just(Boolean.TRUE));
    }
}