package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.spec.Promise;
import io.github.vipcxj.jasync.spec.annotations.Async;
import io.github.vipcxj.jasync.reactive.Promises;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Supplier;

public class Apple1 {

    public static class A {

    }

    private String a;

    private void m() {

    }

    private Promise<String> say(String message) {
        return Promise.just(message);
    }


    public static class B {

    }

    private String b;
}
