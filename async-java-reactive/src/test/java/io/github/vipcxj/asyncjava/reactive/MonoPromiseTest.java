package io.github.vipcxj.asyncjava.reactive;

import io.github.vipcxj.asyncjava.Promise;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class MonoPromiseTest {

    private boolean always() {
        return true;
    }

    @Test
    public void test1() {
        new MonoPromise<>(Mono.delay(Duration.ofSeconds(1)).thenReturn(1))
                .then(v -> {
                    System.out.println(v);
                    if (always()) {
                        throw new RuntimeException("a");
                    } else {
                        return MonoPromise.just(3);
                    }
                })
                .doCatch(t -> {
                    System.out.println(t.getMessage());
                    return MonoPromise.just(2);
                })
                .doFinally(() -> {
                    System.out.println("finally");
                })
                .then((Consumer<Integer>) System.out::println)
                .then(() -> MonoPromise.just(0))
                .then(i -> {
                    AtomicInteger iRef = new AtomicInteger(i);
                    return MonoPromise.just().doWhile(
                            () -> true,
                            () -> {
                                iRef.set(iRef.get() + 1);
                                System.out.println(iRef.get());
                                if (iRef.get() == 3) {
                                    Promise.doReturn(iRef.get());
                                }
                                return null;
                            }
                    );
                })
                .<Integer>doReturn()
                .then((Consumer<Integer>) System.out::println)
                .block();
    }
}
