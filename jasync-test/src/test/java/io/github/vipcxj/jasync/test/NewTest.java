package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.spec.JPromise2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NewTest {
    private JPromise2<Integer> one() {
        return JPromise2.just(1);
    }

    private JPromise2<Integer> two() {
        return JPromise2.just(2);
    }

    private JPromise2<String> sayHello() {
        return JPromise2.just("hello");
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

    private JPromise2<String> oneStaticNew() {
        StaticClassWithArgs object = new StaticClassWithArgs(sayHello().await(), one().await() + two().await());
        return JPromise2.just(object.getMessage() + object.getNumber());
    }

    @Test
    public void testOneStaticNew() throws InterruptedException {
        Assertions.assertEquals("hello3", oneStaticNew().block());
    }

    private JPromise2<String> oneInnerNew() {
        InnerClass object = new InnerClass(sayHello().await(), one().await() + two().await());
        return JPromise2.just(object.getMessage() + object.getNumber());
    }

    @Test
    public void testOneInnerNew() throws InterruptedException {
        Assertions.assertEquals("hello3", oneInnerNew().block());
    }

    private JPromise2<String> nestNews() {
        InnerClass object = new InnerClass(
                new StaticClassWithArgs(sayHello().await(), 1).getMessage(),
                new InnerClass(new StaticClassWithOutArgs().getMessage(), JPromise2.just(new InnerClass()).await().getNumber()).getNumber()
        );
        return JPromise2.just(object.getMessage() + object.getNumber());
    }

    @Test
    public void testNestNews() throws InterruptedException {
        Assertions.assertEquals("hello0", nestNews().block());
    }
}
