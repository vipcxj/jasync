package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.reactive.Promises;
import io.github.vipcxj.jasync.spec.Promise;
import io.github.vipcxj.jasync.spec.annotations.Async;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class Banana {

    public Promise<String> waitAndSay(String message, Duration duration) {
        return Promises.from(Mono.just(message).delayElement(duration));
    }

    @Async
    public Promise<Void> testForeach() {
        String[] messages = new String[] {"apple", "orange", "banana"};
        int index = 0;
        for (String message : messages) {
            System.out.println("" + ++index + ": " + waitAndSay(message, Duration.ofSeconds(1)).await());
        }
        return null;
    }

    public static void main(String[] args) {
        Banana banana = new Banana();
        banana.testForeach().block();
    }
}
