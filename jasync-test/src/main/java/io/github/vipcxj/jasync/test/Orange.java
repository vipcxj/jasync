package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.core.CompareUseCase;
import io.github.vipcxj.jasync.core.javac.Constants;
import io.github.vipcxj.jasync.spec.JAsync;
import io.github.vipcxj.jasync.spec.Promise;
import io.github.vipcxj.jasync.spec.annotations.Async;
import io.github.vipcxj.jasync.spec.functional.PromiseFunction;

public class Orange {

    private String stringField;
    private Promise<String> say(String message) {
        return JAsync.just(message);
    }

    @Async
    public Promise<Void> testSimple() {
        String message = "say";
        String say = say(message).await();
        message = "hello";
        String hello = say(message).await();
        System.out.println(say + " " + hello);
        return null;
    }

    @CompareUseCase
    public Promise<Void> _testSimple() {
        String message = "say";
        final io.github.vipcxj.jasync.runtime.helpers.ObjectReference<java.lang.String> tmp$$4 = new <java.lang.String>io.github.vipcxj.jasync.runtime.helpers.ObjectReference(message, 0);return io.github.vipcxj.jasync.spec.JAsync.deferVoid(new io.github.vipcxj.jasync.spec.functional.VoidPromiseSupplier(){@Override() public Promise<Void> get() throws java.lang.Throwable { final io.github.vipcxj.jasync.runtime.helpers.ObjectReference<java.lang.String> message = tmp$$4;return say(message.getValue()).thenVoid(new io.github.vipcxj.jasync.spec.functional.VoidPromiseFunction<java.lang.String>(){@Override() public Promise<Void> apply(java.lang.String tmp$$0) throws java.lang.Throwable { String say = tmp$$0;
                        message.setAndGet("hello");
                        final java.lang.String tmp$$3 = message.getValue();return io.github.vipcxj.jasync.spec.JAsync.deferVoid(new io.github.vipcxj.jasync.spec.functional.VoidPromiseSupplier(){@Override() public Promise<Void> get() throws java.lang.Throwable { final java.lang.String message = tmp$$3;return say(message).thenVoid(new io.github.vipcxj.jasync.spec.functional.VoidPromiseFunction<java.lang.String>(){@Override() public Promise<Void> apply(java.lang.String tmp$$1) throws java.lang.Throwable { String hello = tmp$$1;
                                        System.out.println(say + " " + hello);
                                        return io.github.vipcxj.jasync.spec.JAsync.doReturn(null);
                                    }
                                });
                            }
                        });
                    }
                });
            }
        }).catchReturn();
    }

    @CompareUseCase
    public <T> T test(int a, String b, T c) {
        int la = 1, lb = 2;
        String lsa = "1", lsb = "2";
        testStart:;
        String ta, tb;
        ta = lsa;
        tb = lsb;
        int tia = la;
        new PromiseFunction<Integer, Void>() {
            @Override
            public Promise<Void> apply(Integer integer) throws Throwable {
                String la = lsa + stringField + ta + tb;
                System.out.println(la + tia + lb + a + b + Constants.ASYNC);
                return null;
            }
        };
        testEnd:;
        return c;
    }

    public static void main(String[] args) {
        Orange orange = new Orange();
        orange.testSimple().block();
    }
}
