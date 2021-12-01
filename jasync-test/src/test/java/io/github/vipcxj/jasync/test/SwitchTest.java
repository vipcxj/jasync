package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.spec.JPromise2;
import io.github.vipcxj.jasync.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class SwitchTest {

    @Async
    private JPromise2<Integer> testReturn(int i) {
        int res;
        switch (i) {
            case 1:
            case 2:
                res = JPromise2.just(i).await();
                return JPromise2.just(res);
            case 3:
            case 4:
                res = JPromise2.just(i * 2).await();
                return JPromise2.just(res);
            default:
                res = JPromise2.just(0).await();
                return JPromise2.just(res);
        }
    }

    @Async
    private JPromise2<Integer> testBreak(int i) {
        int res;
        switch (i) {
            case 1:
            case 2:
                res = JPromise2.just(i).await();
                break;
            case 3:
            case 4:
                res = JPromise2.just(i * 2).await();
                break;
            default:
                res = JPromise2.just(0).await();
        }
        return JPromise2.just(res);
    }

    @SuppressWarnings("UnusedAssignment")
    @Async
    private JPromise2<Integer> testNoBreak1(int i) {
        int res;
        switch (i) {
            case 1:
            case 2:
                res = JPromise2.just(i).await();
            case 3:
            case 4:
                res = JPromise2.just(i * 2).await();
            default:
                res = JPromise2.just(0).await();
        }
        return JPromise2.just(res);
    }

    @SuppressWarnings("UnusedAssignment")
    @Async
    private JPromise2<Integer> testNoBreak2(int i) {
        int res;
        switch (i) {
            default:
                res = JPromise2.just(0).await();
                break;
            case 1:
            case 2:
                res = JPromise2.just(i).await();
            case 3:
            case 4:
                res = JPromise2.just(i * 2).await();
        }
        return JPromise2.just(res);
    }

    @SuppressWarnings("UnusedAssignment")
    @Async
    private JPromise2<Integer> testNoBreak3(int i) {
        int res = -3;
        switch (i) {
            default:
                res = JPromise2.just(0).await();
            case 1:
            case 2:
                res = JPromise2.just(i).await();
            case 3:
                break;
            case 4:
                res = JPromise2.just(i * 2).await();
        }
        return JPromise2.just(res);
    }

    @ParameterizedTest
    @DisplayName("test return: if i = 1 or 2, return i; if i = 3 or 4, return 2 * i; else return 0")
    @ValueSource(ints = {-1, 0, 1, 2, 3, 4, 5, 6})
    void test1(int i) {
        if (i == 1 || i == 2) {
            Assertions.assertEquals(i, testReturn(i).block());
        } else if (i == 3 || i == 4) {
            Assertions.assertEquals(2 * i, testReturn(i).block());
        } else {
            Assertions.assertEquals(0, testReturn(i).block());
        }
    }

    @ParameterizedTest
    @DisplayName("test break: if i = 1 or 2, return i; if i = 3 or 4, return 2 * i; else return 0")
    @ValueSource(ints = {-1, 0, 1, 2, 3, 4, 5, 6})
    void test2(int i) {
        if (i == 1 || i == 2) {
            Assertions.assertEquals(i, testBreak(i).block());
        } else if (i == 3 || i == 4) {
            Assertions.assertEquals(2 * i, testBreak(i).block());
        } else {
            Assertions.assertEquals(0, testBreak(i).block());
        }
    }

    @ParameterizedTest
    @DisplayName("test no break 1: always return 0")
    @ValueSource(ints = {-1, 0, 1, 2, 3, 4, 5, 6})
    void test3(int i) {
        Assertions.assertEquals(0, testNoBreak1(i).block());
    }

    @ParameterizedTest
    @DisplayName("test no break 2: if i = 1 or 2 or 3 or 4, return 2 * i; else return 0")
    @ValueSource(ints = {-1, 0, 1, 2, 3, 4, 5, 6})
    void test4(int i) {
        if (i >= 1 && i <= 4) {
            Assertions.assertEquals(2 * i, testNoBreak2(i).block());
        } else {
            Assertions.assertEquals(0, testNoBreak2(i).block());
        }
    }

    @ParameterizedTest
    @DisplayName("test no break 3: if i = 3, return -3; if i = 4, return 8; else return 2 * i")
    @ValueSource(ints = {-1, 0, 1, 2, 3, 4, 5, 6})
    void test5(int i) {
        if (i == 3) {
            Assertions.assertEquals(-3, testNoBreak3(i).block());
        } else if (i == 4) {
            Assertions.assertEquals(8, testNoBreak3(i).block());
        } else {
            Assertions.assertEquals(i, testNoBreak3(i).block());
        }
    }
}
