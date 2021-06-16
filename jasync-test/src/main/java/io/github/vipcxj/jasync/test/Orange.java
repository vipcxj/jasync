package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.core.CompareUseCase;
import io.github.vipcxj.jasync.spec.JAsync;
import io.github.vipcxj.jasync.spec.Promise;
import io.github.vipcxj.jasync.spec.annotations.Async;

public class Orange {

    private Promise<String> say(String message) {
        return JAsync.just(message);
    }

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
        final io.github.vipcxj.jasync.spec.helpers.ObjectReference<java.lang.String> tmp$$4 = new <java.lang.String>io.github.vipcxj.jasync.spec.helpers.ObjectReference(message, 0);return io.github.vipcxj.jasync.spec.JAsync.deferVoid(new io.github.vipcxj.jasync.spec.functional.VoidPromiseSupplier(){@Override() public Promise<Void> get() throws java.lang.Throwable { final io.github.vipcxj.jasync.spec.helpers.ObjectReference<java.lang.String> message = tmp$$4;return say(message.getValue()).thenVoid(new io.github.vipcxj.jasync.spec.functional.VoidPromiseFunction<java.lang.String>(){@Override() public Promise<Void> apply(java.lang.String tmp$$0) throws java.lang.Throwable { String say = tmp$$0;
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

    public static void main(String[] args) {
        Orange orange = new Orange();
        orange.testSimple().block();
    }
}
