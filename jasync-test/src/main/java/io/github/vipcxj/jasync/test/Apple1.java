package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.spec.JAsync;
import io.github.vipcxj.jasync.spec.Promise;

import java.io.StringWriter;

public class Apple1 {

    public static class A {

    }

    private String a;

    private void m() {

    }

    private Promise<StringWriter> getWriter() {
        return JAsync.just(new StringWriter());
    }

    private Promise<String> say(String message) {
        return JAsync.just(message);
    }


    public static class B {

    }

    private String b;
}
