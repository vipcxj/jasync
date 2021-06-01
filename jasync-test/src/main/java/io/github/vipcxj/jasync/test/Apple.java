package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.spec.Promise;
import io.github.vipcxj.jasync.spec.annotations.Async;
import io.github.vipcxj.jasync.reactive.Promises;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
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

    @Async
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

    public static String getLineInfo() {
        StackTraceElement ste = new Throwable().getStackTrace()[1];
        return ste.getFileName() + ": Line " + ste.getLineNumber();
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

    private Promise<StringWriter> getWriter() {
        return Promise.just(new StringWriter());
    }

    @Async
    public Promise<Void> testTryWith() {
        try (
                StringWriter writer1 = getWriter().await();
                StringWriter writer2 = new StringWriter();
                StringWriter writer3 = getWriter().await();
        ){
            writer1.append('1');
            writer2.append('2');
            writer3.append(say("3").await());
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            System.out.println('3');
        }
        return Promise.just();
    }

    public void test() {
        try (
                StringWriter writer1 = new StringWriter();
                StringWriter writer2 = new StringWriter();
                StringWriter writer3 = new StringWriter();
        ){
            writer1.append('1');
            writer2.append('2');
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            System.out.println('3');
        }
    }

    public static void main(String[] args) {
        Apple apple = new Apple();
        apple.test2(3).block();
    }

    public static class B {

    }

    private String b;
}
