package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.runtime.helpers.IntReference;
import io.github.vipcxj.jasync.runtime.java8.helpers.IndyHelper;
import io.github.vipcxj.jasync.runtime.java8.helpers.IndyHelpers;
import io.github.vipcxj.jasync.spec.JAsync;
import io.github.vipcxj.jasync.spec.Promise;
import io.github.vipcxj.jasync.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class WhileTest1 {

    private final static IndyHelpers indyHelpers = new IndyHelpers(MethodHandles.lookup());
    private final IndyHelper indyHelper = indyHelpers.instance(this);

    private Promise<Void> lambda1(IntReference sum1, int to) {
        return JAsync.doWhile(
                indyHelper.booleanSupplier("lambda2", MethodType.methodType(boolean.class, IntReference.class, int.class), sum1, to),
                indyHelper.voidPromiseSupplier("lambda3", MethodType.methodType(Promise.class, IntReference.class), sum1)
        ).thenVoid(indyHelper.voidPromiseSupplier("lambda4", MethodType.methodType(Promise.class, IntReference.class), sum1));
    }

    private boolean lambda2(IntReference sum1, int to) {
        return (sum1.getValue() < to);
    }

    private Promise<Void> lambda3(IntReference sum1) {
        return JAsync.just(1).thenVoid(
                indyHelper.voidPromiseFunction("lambda5", MethodType.methodType(Promise.class, IntReference.class, int.class), sum1)
        );
    }

    private Promise<Void> lambda4(IntReference sum1) {
        return JAsync.doReturn(JAsync.just(sum1.getValue()));
    }

    private Promise<Void> lambda5(IntReference sum1, int tmp$$1) {
        sum1.addAndGetValue(tmp$$1);
        return null;
    }

    @Async
    public Promise<Integer> sum1(int to) {
        int sum = 0;
        final io.github.vipcxj.jasync.runtime.helpers.IntReference tmp$$5 = new io.github.vipcxj.jasync.runtime.helpers.IntReference(sum);
        return io.github.vipcxj.jasync.spec.JAsync.deferVoid(
                indyHelper.voidPromiseSupplier(
                        "lambda1",
                        MethodType.methodType(
                                Promise.class,
                                IntReference.class,
                                int.class
                        ),
                        tmp$$5,
                        to
                )
        ).catchReturn();
    }

    @Test
    public void test1() {
        Assertions.assertEquals(2000000, sum1(2000000).block());
    }


}
