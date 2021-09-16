package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.spec.JAsync;
import io.github.vipcxj.jasync.spec.JPromise;
import io.github.vipcxj.jasync.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("InnerClassMayBeStatic")
public class InnerClassTest {

    class InnerClass {

        @Async
        private JPromise<String> fun() {
            return JAsync.just(
                    JAsync.just("InnerClass").await()
            );
        }

        class NestInnerClass {

            @Async
            private JPromise<String> fun() {
                return JAsync.just(
                        JAsync.just("InnerClass").await() + "." + JAsync.just("NestInnerClass").await()
                );
            }
        }
    }

    static class StaticInnerClass {

        @Async
        private JPromise<String> fun() {
            return JAsync.just(
                    JAsync.just("StaticInnerClass").await()
            );
        }

        class NestInnerClass {
            @Async
            private JPromise<String> fun() {
                return JAsync.just(
                        JAsync.just("StaticInnerClass").await() + "." + JAsync.just("NestInnerClass").await()
                );
            }
        }

        static class StaticNestInnerClass {
            @Async
            private JPromise<String> fun() {
                return JAsync.just(
                        JAsync.just("StaticInnerClass").await() + "." + JAsync.just("StaticNestInnerClass").await()
                );
            }
        }
    }

    @Test
    public void test() {
        InnerClass innerClass = new InnerClass();
        Assertions.assertEquals("InnerClass", innerClass.fun().block());
        Assertions.assertEquals("InnerClass.NestInnerClass", innerClass.new NestInnerClass().fun().block());
        StaticInnerClass staticInnerClass = new StaticInnerClass();
        Assertions.assertEquals("StaticInnerClass", staticInnerClass.fun().block());
        Assertions.assertEquals("StaticInnerClass.NestInnerClass", staticInnerClass.new NestInnerClass().fun().block());
        Assertions.assertEquals("StaticInnerClass.StaticNestInnerClass", new StaticInnerClass.StaticNestInnerClass().fun().block());
    }

}
