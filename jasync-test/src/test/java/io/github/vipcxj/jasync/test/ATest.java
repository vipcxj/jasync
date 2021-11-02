package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.runtime.promise.ValuePromise;
import io.github.vipcxj.jasync.spec.JPromise2;
import io.github.vipcxj.jasync.spec.annotations.Async;

public class ATest {

    private JPromise2<Integer> one() {
        return JPromise2.just(1);
    }

    private JPromise2<Integer> two() {
        return JPromise2.just(2);
    }

    public JPromise2<Integer> a(int input) {
        Number a = 5;
        Integer one = one().await();
        Integer two = two().await();
        int res = 0;
        label: for (int i = 0; i < 10; ++i) {
            a = a.longValue() - 1;
            for (int j = 0; j < 10; ++j) {
                if (one == 1) {
                    if (two == 2) {
                        a = a.intValue() + 1;
                        res = one + two + new SubPromise<>(3).await();
                    } else if (two == 3) {
                        continue label;
                    } else {
                        break;
                    }
                }
            }
        }
        return JPromise2.just(res);
    }

    @Async
    public JPromise2<Integer> aa() {
        Integer one = JPromise2.just(1).await();
        Integer two = JPromise2.just(2).await();
        return JPromise2.just(one + two);
    }

    public JPromise2<Integer> b() {
        return one().then(one -> two().then(two -> {
            int res = one + two;
            return JPromise2.just(res);
        }));
    }

    public int c() {
        int a = 0;
        int c = 0;
        {
            int b = 1;
            c += b;
        }
        return c;
    }

    static class SubPromise<T> extends ValuePromise<T> {

        public SubPromise(T value) {
            super(value);
        }
    }
}
