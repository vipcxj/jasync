package io.github.vipcxj.jasync.ng.test;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.annotations.Async;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
                    success = doSomething().block();
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
    private JPromise<Boolean> doSomething() {
        JPromise.sleep(100, TimeUnit.MILLISECONDS).await();
        return JPromise.just(true);
    }
}
