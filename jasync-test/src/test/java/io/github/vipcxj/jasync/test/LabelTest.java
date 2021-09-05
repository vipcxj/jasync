package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.spec.JAsync;
import io.github.vipcxj.jasync.spec.Promise;
import io.github.vipcxj.jasync.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("UnusedLabel")
public class LabelTest {

    @Async
    public Promise<Integer> justBreak() {
        int a = 1;
        label: break label;
        return JAsync.just(a);
    }

    @Test
    public void testJustBreak() {
        Assertions.assertEquals(1, justBreak().block());
    }

    @Async
    public Promise<Integer> singleStatementNoAwait() {
        int a = 1;
        label: ++a;
        return JAsync.just(a);
    }

    @Test
    public void testSingleStatementNoAwait() {
        Assertions.assertEquals(2, singleStatementNoAwait().block());
    }

    @Async
    public Promise<Integer> singleStatementHasAwait() {
        int a = 1;
        int b;
        label: b = JAsync.just(a).await() + 1;
        return JAsync.just(b);
    }

    @Test
    public void testSingleStatementHasAwait() {
        Assertions.assertEquals(2, singleStatementHasAwait().block());
    }

    @Async
    public Promise<Integer> ifNoAwait(boolean input) {
        int a = 1;
        label:
        if (input) {
            break label;
        } else {
            ++a;
        }
        return JAsync.just(a);
    }

    @Test
    public void testIfNoAwait() {
        Assertions.assertEquals(1, ifNoAwait(true).block());
        Assertions.assertEquals(2, ifNoAwait(false).block());
    }

    @Async
    public Promise<Integer> ifHasAwait1(boolean input) {
        int a = 1;
        label:
        if (JAsync.just(input).await()) {
            break label;
        } else {
            ++a;
        }
        return JAsync.just(a);
    }

    @Test
    public void testIfHasAwait1() {
        Assertions.assertEquals(1, ifHasAwait1(true).block());
        Assertions.assertEquals(2, ifHasAwait1(false).block());
    }

    @Async
    public Promise<Integer> ifHasAwait2(boolean input) {
        int a = 1;
        label:
        if (input) {
            break label;
        } else {
            a += JAsync.just(1).await();
        }
        return JAsync.just(a);
    }

    @Test
    public void testIfHasAwait2() {
        Assertions.assertEquals(1, ifHasAwait2(true).block());
        Assertions.assertEquals(2, ifHasAwait2(false).block());
    }

    @Async
    public Promise<Integer> ifHasAwait3(boolean input) {
        int a = 1;
        label:
        if (JAsync.just(input).await()) {
            break label;
        } else {
            a += JAsync.just(1).await();
        }
        return JAsync.just(a);
    }

    @Test
    public void testIfHasAwait3() {
        Assertions.assertEquals(1, ifHasAwait3(true).block());
        Assertions.assertEquals(2, ifHasAwait3(false).block());
    }

    @Async
    public Promise<Integer> whileNoAwait1() {
        int a = 1;
        label:
        while (a < 3) {
            if (a == 2) {
                break;
            }
            ++a;
        }
        return JAsync.just(a);
    }

    @Test
    public void testWhileNoAwait1() {
        Assertions.assertEquals(2, whileNoAwait1().block());
    }

    @Async
    public Promise<Integer> whileNoAwait2() {
        int a = 0;
        int i = 0;
        label:
        while (i++ < 10) {
            if (i % 2 == 0) {
                continue;
            }
            int j = 0;
            while (j++ < 10) {
                if (j == i) {
                    break;
                }
                ++a;
            }
        }
        return JAsync.just(a);
    }

    @Test
    public void testWhileNoAwait2() {
        Assertions.assertEquals(20, whileNoAwait2().block());
    }

    @Async
    public Promise<Integer> whileNoAwait3() {
        int a = 0;
        int i = 0;
        label:
        while (i++ < 10) {
            if (i % 2 == 0) {
                continue;
            }
            int j = 0;
            while (j++ < 10) {
                if (j == i) {
                    continue label;
                }
                ++a;
            }
        }
        return JAsync.just(a);
    }

    @Test
    public void testWhileNoAwait3() {
        Assertions.assertEquals(20, whileNoAwait3().block());
    }

    @Async
    public Promise<Integer> whileNoAwait4() {
        int a = 0;
        int i = 0;
        label1:
        while (i++ < 10) {
            int j = 0;
            label2:
            while (j++ < 10) {
                if (j == i) {
                    continue label1;
                }
                int k = 0;
                label3:
                while (k++ < 10) {
                    if (k == j) {
                        break label2;
                    }
                    ++a;
                }
            }
            j = 0;
            label2:
            while (j++ < 10) {
                int k = 0;
                label3:
                while (k++ < 10) {
                    if (i == k) {
                        break label2;
                    }
                    ++a;
                }
            }
        }
        return JAsync.just(a);
    }

    @Test
    public void testWhileNoAwait4() {
        Assertions.assertEquals(45, whileNoAwait4().block());
    }

    @Async
    public Promise<Integer> whileHasAwait1() {
        int a = 1;
        label:
        while (a < JAsync.just(3).await()) {
            if (a == JAsync.just(2).await()) {
                break;
            }
            ++a;
        }
        return JAsync.just(a);
    }

    @Test
    public void testWhileHasAwait1() {
        Assertions.assertEquals(2, whileHasAwait1().block());
    }

    @Async
    public Promise<Integer> whileHasAwait2() {
        int a = 0;
        int i = 0;
        label:
        while (i++ < 10) {
            if (i % JAsync.just(2).await() == 0) {
                continue;
            }
            int j = 0;
            while (j++ < JAsync.just(10).await()) {
                if (JAsync.just(j).await() == i) {
                    break;
                }
                ++a;
            }
        }
        return JAsync.just(a);
    }

    @Test
    public void testWhileHasAwait2() {
        Assertions.assertEquals(20, whileHasAwait2().block());
    }

    @Async
    public Promise<Integer> whileHasAwait3() {
        int a = 0;
        int i = 0;
        label:
        while (i++ < JAsync.just(10).await()) {
            if (i % JAsync.just(2).await() == 0) {
                continue;
            }
            int j = 0;
            while (j++ < JAsync.just(10).await()) {
                if (JAsync.just(j).await().equals(JAsync.just(i).await())) {
                    continue label;
                }
                ++a;
            }
        }
        return JAsync.just(a);
    }

    @Test
    public void testWhileHasAwait3() {
        Assertions.assertEquals(20, whileHasAwait3().block());
    }

    @Async
    public Promise<Integer> whileHasAwait4() {
        int a = 0;
        int i = 0;
        label1:
        while (i++ < JAsync.just(10).await()) {
            int j = JAsync.just(0).await();
            label2:
            while (j++ < 10) {
                if (j == JAsync.just(i).await()) {
                    continue label1;
                }
                int k = 0;
                int ten = JAsync.just(10).await();
                label3:
                while (k++ < ten) {
                    if (k == JAsync.just(j).await()) {
                        break label2;
                    }
                    ++a;
                }
            }
            j = JAsync.just(0).await();
            label2:
            while (j++ < JAsync.just(10).await()) {
                int k = 0;
                label3:
                while (k++ < 10) {
                    if (JAsync.just(i).await().equals(JAsync.just(k).await())) {
                        break label2;
                    }
                    ++a;
                }
            }
        }
        return JAsync.just(a);
    }

    @Test
    public void testWhileHasAwait4() {
        Assertions.assertEquals(45, whileHasAwait4().block());
    }

    @Async
    public Promise<Integer> doWhileNoAwait1() {
        int a = 1;
        label:
        do {
            if (a == 2) {
                break;
            }
            ++a;
        }
        while (a < 3);
        return JAsync.just(a);
    }

    @Test
    public void testDoWhileNoAwait1() {
        Assertions.assertEquals(2, doWhileNoAwait1().block());
    }

    @Async
    public Promise<Integer> doWhileNoAwait2() {
        int a = 0;
        int i = 0;
        label:
        do {
            if (i % 2 == 0) {
                continue;
            }
            int j = 0;
            do {
                if (j == i) {
                    break;
                }
                ++a;
            }
            while (j++ < 10);
        }
        while (i++ < 10);
        return JAsync.just(a);
    }

    @Test
    public void testDoWhileNoAwait2() {
        Assertions.assertEquals(25, doWhileNoAwait2().block());
    }

    @Async
    public Promise<Integer> doWhileNoAwait3() {
        int a = 0;
        int i = 0;
        label:
        do {
            if (i % 2 == 0) {
                continue;
            }
            int j = 0;
            do {
                if (j == i) {
                    continue label;
                }
                ++a;
            }
            while (j++ < 10);
        }
        while (i++ < 10);
        return JAsync.just(a);
    }

    @Test
    public void testDoWhileNoAwait3() {
        Assertions.assertEquals(25, doWhileNoAwait3().block());
    }

    @Async
    public Promise<Integer> doWhileNoAwait4() {
        int a = 0;
        int i = 0;
        label1:
        do {
            int j = 0;
            label2:
            do {
                if (j == i) {
                    continue label1;
                }
                int k = 0;
                label3:
                do {
                    if ((k + j) % 5 == 4) {
                        break label2;
                    } else if ((k + j) % 5 == 1) {
                        continue label2;
                    }
                    ++a;
                    ++k;
                }
                while (true);
            }
            while (j++ < 10);
            j = 0;
            label2:
            do {
                int k = 0;
                label3:
                do {
                    if (i == k) {
                        break label2;
                    }
                    ++a;
                }
                while (k++ < 10);
            }
            while (j++ < 10);
        }
        while (i++ < 10);
        return JAsync.just(a);
    }

    @Test
    public void testDoWhileNoAwait4() {
        Assertions.assertEquals(78, doWhileNoAwait4().block());
    }

    @Async
    public Promise<Integer> doWhileHasAwait1() {
        int a = 1;
        label:
        do {
            if (a == JAsync.just(2).await()) {
                break;
            }
            ++a;
        }
        while (a < JAsync.just(3).await());
        return JAsync.just(a);
    }

    @Test
    public void testDoWhileHasAwait1() {
        Assertions.assertEquals(2, doWhileHasAwait1().block());
    }

    @Async
    public Promise<Integer> doWhileHasAwait2() {
        int a = 0;
        int i = 0;
        label:
        do {
            if (i % JAsync.just(2).await() == 0) {
                continue;
            }
            int j = 0;
            do {
                if (JAsync.just(j).await() == i) {
                    break;
                }
                ++a;
            }
            while (j++ < JAsync.just(10).await());
        }
        while (i++ < 10);
        return JAsync.just(a);
    }

    @Test
    public void testDoWhileHasAwait2() {
        Assertions.assertEquals(25, doWhileHasAwait2().block());
    }

    @Async
    public Promise<Integer> doWhileHasAwait3() {
        int a = 0;
        int i = 0;
        label:
        do {
            if (i % JAsync.just(2).await() == 0) {
                continue;
            }
            int j = 0;
            do {
                if (JAsync.just(j).await().equals(JAsync.just(i).await())) {
                    continue label;
                }
                ++a;
            }
            while (j++ < JAsync.just(10).await());
        }
        while (i++ < JAsync.just(10).await());
        return JAsync.just(a);
    }

    @Test
    public void testDoWhileHasAwait3() {
        Assertions.assertEquals(25, doWhileHasAwait3().block());
    }

    @Async(debug = true)
    public Promise<Integer> doWhileHasAwait4() {
        int a = 0;
        int i = 0;
        label1:
        do {
            int j = JAsync.just(0).await();
            label2:
            do {
                boolean test = JAsync.just(i).await().equals(JAsync.just(j).await());
                if (test) {
                    continue label1;
                }
                int k = JAsync.just(0).await();
                label3:
                do {
                    if ((k + JAsync.just(j).await()) % 5 == 4) {
                        break label2;
                    } else if ((JAsync.just(k).await() + j) % 5 == 1) {
                        continue label2;
                    }
                    ++a;
                    ++k;
                }
                while (true);
            }
            while (j++ < JAsync.just(10).await());
            j = 0;
            label2:
            do {
                int k = JAsync.just(0).await();
                label3:
                do {
                    if (i == k) {
                        break label2;
                    }
                    ++a;
                }
                while (k++ < JAsync.just(10).await());
            }
            while (j++ < JAsync.just(10).await());
        }
        while (i++ < 10);
        return JAsync.just(a);
    }

    @Test
    public void testDoWhileHasAwait4() {
        Assertions.assertEquals(78, doWhileHasAwait4().block());
    }

    @Async
    public Promise<Integer> forNoAwait1(int num) {
        int a = 0;
        label:
        for (int i = 0; i < num; ++i) {
            if (i % 2 == 0) {
                continue;
            }
            if (i == 5) {
                break;
            }
            ++a;
        }
        return JAsync.just(a);
    }

    @Test
    public void testForNoAwait1() {
        for (int i = 0; i < 10; ++i) {
            if (i <= 5) {
                Assertions.assertEquals(i / 2, forNoAwait1(i).block());
            } else {
                Assertions.assertEquals(2, forNoAwait1(i).block());
            }
        }
    }

    @Async
    public Promise<Integer> forNoAwait2(int num) {
        int a = 0;
        label1:
        for (int i = 0; i < num; ++i) {
            label2:
            for (int j = 0; j < num; ++j) {
                if (i == j) {
                    if (i < 5) {
                        continue label1;
                    } else {
                        continue;
                    }
                }
                ++a;
            }
            label2:
            for (int j = 0; j < num; ++j) {
                ++a;
                if (i == j) {
                    if (i < 5) {
                        continue label1;
                    } else if (i < 7) {
                        continue;
                    } else if (i < 9) {
                        break label1;
                    } else {
                        break;
                    }
                }
                ++a;
            }
        }
        return JAsync.just(a);
    }

    @Test
    public void testForNoAwait2() {
        for (int i = 0; i < 12; ++i) {
            if (i <= 5) {
                Assertions.assertEquals(i * (i -1) / 2, forNoAwait2(i).block());
            } else if (i == 6) {
                Assertions.assertEquals(26, forNoAwait2(i).block());
            } else if (i == 7) {
                Assertions.assertEquals(48, forNoAwait2(i).block());
            } else if (i == 8) {
                Assertions.assertEquals(76, forNoAwait2(i).block());
            } else if (i == 9) {
                Assertions.assertEquals(83, forNoAwait2(i).block());
            } else if (i == 10) {
                Assertions.assertEquals(90, forNoAwait2(i).block());
            } else {
                Assertions.assertEquals(97, forNoAwait2(i).block());
            }
        }
    }

    @Async
    public Promise<Integer> forHasAwait1(int num) {
        int a = 0;
        label:
        for (int i = JAsync.just(0).await(); i < JAsync.just(num).await(); ++i) {
            if (JAsync.just(i).await() % 2 == JAsync.just(0).await()) {
                continue;
            }
            if (i == JAsync.just(5).await()) {
                break;
            }
            ++a;
        }
        return JAsync.just(a);
    }

    @Test
    public void testForHasAwait1() {
        for (int i = 0; i < 10; ++i) {
            if (i <= 5) {
                Assertions.assertEquals(i / 2, forHasAwait1(i).block());
            } else {
                Assertions.assertEquals(2, forHasAwait1(i).block());
            }
        }
    }

    @Async
    public Promise<Integer> forHasAwait2(int num) {
        int a = 0;
        label1:
        for (int i = 0; JAsync.just(i).await() < num; ++i) {
            label2:
            for (int j = JAsync.just(0).await(); j < JAsync.just(num).await(); ++j) {
                if (i == JAsync.just(j).await()) {
                    if (JAsync.just(i).await() < JAsync.just(5).await()) {
                        continue label1;
                    } else {
                        continue;
                    }
                }
                ++a;
            }
            label2:
            for (int j = 0; j < num; ++j) {
                ++a;
                if (i == JAsync.just(j).await()) {
                    if (i < 5) {
                        continue label1;
                    } else if (JAsync.just(i).await() < 7) {
                        continue;
                    } else if (i < JAsync.just(9).await()) {
                        break label1;
                    } else {
                        break;
                    }
                }
                ++a;
            }
        }
        return JAsync.just(a);
    }

    @Test
    public void testForHasAwait2() {
        for (int i = 0; i < 12; ++i) {
            if (i <= 5) {
                Assertions.assertEquals(i * (i -1) / 2, forHasAwait2(i).block());
            } else if (i == 6) {
                Assertions.assertEquals(26, forHasAwait2(i).block());
            } else if (i == 7) {
                Assertions.assertEquals(48, forHasAwait2(i).block());
            } else if (i == 8) {
                Assertions.assertEquals(76, forHasAwait2(i).block());
            } else if (i == 9) {
                Assertions.assertEquals(83, forHasAwait2(i).block());
            } else if (i == 10) {
                Assertions.assertEquals(90, forHasAwait2(i).block());
            } else {
                Assertions.assertEquals(97, forHasAwait2(i).block());
            }
        }
    }
}
