package io.github.vipcxj.jasync.ng.test;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NewTest {
    private JPromise<Integer> one() {
        return JPromise.just(1);
    }

    private JPromise<Integer> two() {
        return JPromise.just(2);
    }

    private JPromise<String> sayHello() {
        return JPromise.just("hello");
    }

    public static class StaticClassWithOutArgs {
        public StaticClassWithOutArgs() {}

        public String getMessage() {
            return "not hello";
        }
    }

    public static class StaticClassWithArgs {
        private final String message;
        private final Integer number;

        public StaticClassWithArgs(String message, Integer number) {
            this.message = message;
            this.number = number;
        }

        public String getMessage() {
            return message;
        }

        public Integer getNumber() {
            return number;
        }
    }

    @SuppressWarnings("InnerClassMayBeStatic")
    public class InnerClass {
        private final String message;
        private final Integer number;

        public InnerClass() {
            this.message = "hello";
            this.number = 0;
        }

        public InnerClass(String message, Integer number) {
            this.message = message;
            this.number = number;
        }

        public String getMessage() {
            return message;
        }

        public Integer getNumber() {
            return number;
        }
    }

    private JPromise<String> oneStaticNew() {
        StaticClassWithArgs object = new StaticClassWithArgs(sayHello().await(), one().await() + two().await());
        return JPromise.just(object.getMessage() + object.getNumber());
    }

    @Test
    public void testOneStaticNew() throws InterruptedException {
        Assertions.assertEquals("hello3", oneStaticNew().block());
    }

    private JPromise<String> oneInnerNew() {
        InnerClass object = new InnerClass(sayHello().await(), one().await() + two().await());
        return JPromise.just(object.getMessage() + object.getNumber());
    }

    @Test
    public void testOneInnerNew() throws InterruptedException {
        Assertions.assertEquals("hello3", oneInnerNew().block());
    }

    private JPromise<String> nestNews() {
        InnerClass object = new InnerClass(
                new StaticClassWithArgs(sayHello().await(), 1).getMessage(),
                new InnerClass(new StaticClassWithOutArgs().getMessage(), JPromise.just(new InnerClass()).await().getNumber()).getNumber()
        );
        return JPromise.just(object.getMessage() + object.getNumber());
    }

    @Test
    public void testNestNews() throws InterruptedException {
        Assertions.assertEquals("hello0", nestNews().block());
    }
}
