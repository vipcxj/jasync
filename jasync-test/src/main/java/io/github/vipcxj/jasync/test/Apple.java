package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.spec.Promise;
import io.github.vipcxj.jasync.spec.annotations.Async;
import io.github.vipcxj.jasync.reactive.Promises;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class Apple {

    public static class A {

    }

    private String a;

    private void m() {

    }

    private Promise<String> say(String message) {
        return Promise.just(message);
    }

    public Promise<Boolean> test1() {
        Integer a = Promises.from(Mono.just(6).delayElement(Duration.ofSeconds(1))).await();
        if (a > 3) {
            try {
                System.out.println(say("hello").await());
            } catch (Throwable t) {
                return Promise.error(t);
            } finally {
                System.out.println(say("finally").await());
            }
            return Promise.just(true);
        } else if (a == 2) {
            say("a = 2").await();
        } else {
            say("else").await();
        }
        return Promise.just(false);
    }

    @Async
    public Promise<Integer> test2(int a) {
        int i = 0, j, o;
        Double k;
        j = 1;
        ++a;
        o = 2;
        Double p = (double) o;
        say("something.").await();
        k = 3.0;
        --a;
        System.out.println(p);
        o = 1;
        return Promise.just(a + i + j + k.intValue());
    }

    public static void main(String[] args) {
        Apple apple = new Apple();
        apple.test2(3).block();
    }

    public static class B {

    }

    private String b;
}
