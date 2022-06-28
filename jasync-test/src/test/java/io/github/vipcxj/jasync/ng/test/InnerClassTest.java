package io.github.vipcxj.jasync.ng.test;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("InnerClassMayBeStatic")
public class InnerClassTest {

    class InnerClass {

        @Async
        private JPromise<String> fun() throws InterruptedException {
            return JPromise.just(
                    JPromise.just("InnerClass").await()
            );
        }

        class NestInnerClass {

            private JPromise<String> fun() throws InterruptedException {
                return JPromise.just(
                        JPromise.just("InnerClass").await() + "." + JPromise.just("NestInnerClass").await()
                );
            }

            public JPromise<String> closingMethod() throws InterruptedException {
                class AnClass {
                    public JPromise<String> fun() {
                        return JPromise.just("InnerClass.NestInnerClass.closingMethod.AnClass");
                    }

                    public JPromise<String> closingMethod() {
                        class BnClass {
                            public JPromise<String> fun() {
                                return JPromise.just("InnerClass.NestInnerClass.closingMethod.AnClass.closingMethod.BnClass");
                            }
                        }
                        return new BnClass().fun();
                    }
                }
                AnClass anClass = new AnClass();
                String result = anClass.fun().await() + " and " + anClass.closingMethod().await();
                return JPromise.just(result);
            }
        }
    }

    static class StaticInnerClass {

        @Async
        private JPromise<String> fun() throws InterruptedException {
            return JPromise.just(
                    JPromise.just("StaticInnerClass").await()
            );
        }

        class NestInnerClass {
            @Async
            private JPromise<String> fun() throws InterruptedException {
                return JPromise.just(
                        JPromise.just("StaticInnerClass").await() + "." + JPromise.just("NestInnerClass").await()
                );
            }
        }

        static class StaticNestInnerClass {
            @Async
            private JPromise<String> fun() throws InterruptedException {
                return JPromise.just(
                        JPromise.just("StaticInnerClass").await() + "." + JPromise.just("StaticNestInnerClass").await()
                );
            }
        }
    }

    @Test
    public void test() throws InterruptedException {
        InnerClass innerClass = new InnerClass();
        Assertions.assertEquals("InnerClass", innerClass.fun().block());
        Assertions.assertEquals("InnerClass.NestInnerClass", innerClass.new NestInnerClass().fun().block());
        Assertions.assertEquals("InnerClass.NestInnerClass.closingMethod.AnClass and InnerClass.NestInnerClass.closingMethod.AnClass.closingMethod.BnClass", innerClass.new NestInnerClass().closingMethod().block());
        StaticInnerClass staticInnerClass = new StaticInnerClass();
        Assertions.assertEquals("StaticInnerClass", staticInnerClass.fun().block());
        Assertions.assertEquals("StaticInnerClass.NestInnerClass", staticInnerClass.new NestInnerClass().fun().block());
        Assertions.assertEquals("StaticInnerClass.StaticNestInnerClass", new StaticInnerClass.StaticNestInnerClass().fun().block());
    }

}
