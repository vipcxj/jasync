package io.github.vipcxj.jasync.ng.test;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class StaticInitMergerTest {

    private static final long ONE;

    private static long getStaticNum() {
        return ONE;
    }

    static {
        ONE = 1;
    }

    private static final int TWO = 2;

    private static int getIntEnum() {
        return TWO;
    }

    private JPromise<Integer> one() {
        return JPromise.just(1);
    }

    private JPromise<Integer> simpleLoop() throws InterruptedException {
        int sum = 0;
        for (int i = 0; i < 10; ++i) {
            sum += one().await();
        }
        return JPromise.just(sum);
    }

    @Test
    public void testSimpleLoop() throws InterruptedException {
        Assertions.assertEquals(10, simpleLoop().block());
    }

    static {
        try {
            long staticNum = getStaticNum();
            if (staticNum != 1) {
                throw new IOException();
            }
            switch (getIntEnum()) {
                case 1:
                case 2:
                    break;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
