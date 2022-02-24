package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.spec.JPromise2;
import io.github.vipcxj.jasync.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings({"UnusedLabel", "ConstantConditions"})
public class LabelTest {
 
    @Async
    public JPromise2<Integer> justBreak() {
        int a = 1;
        label: break label;
        return JPromise2.just(a);
        
    }

    @Test
    public void testJustBreak() throws InterruptedException {
        Assertions.assertEquals(1, justBreak().block());
    }

    @Async
    public JPromise2<Integer> singleStatementNoAwait() {
        int a = 1;
        label: ++a;
        return JPromise2.just(a);
    }

    @Test
    public void testSingleStatementNoAwait() throws InterruptedException {
        Assertions.assertEquals(2, singleStatementNoAwait().block());
    }

    @Async
    public JPromise2<Integer> singleStatementHasAwait() {
        int a = 1;
        int b;
        label: b = JPromise2.just(a).await() + 1;
        return JPromise2.just(b);
    }

    @Test
    public void testSingleStatementHasAwait() throws InterruptedException {
        Assertions.assertEquals(2, singleStatementHasAwait().block());
    }

    @Async
    public JPromise2<Integer> ifNoAwait(boolean input) {
        int a = 1;
        label:
        if (input) {
            break label;
        } else {
            ++a;
        }
        return JPromise2.just(a);
    }

    @Test
    public void testIfNoAwait() throws InterruptedException {
        Assertions.assertEquals(1, ifNoAwait(true).block());
        Assertions.assertEquals(2, ifNoAwait(false).block());
    }

    @Async
    public JPromise2<Integer> ifHasAwait1(boolean input) {
        int a = 1;
        label:
        if (JPromise2.just(input).await()) {
            break label;
        } else {
            ++a;
        }
        return JPromise2.just(a);
    }

    @Test
    public void testIfHasAwait1() throws InterruptedException {
        Assertions.assertEquals(1, ifHasAwait1(true).block());
        Assertions.assertEquals(2, ifHasAwait1(false).block());
    }

    @Async
    public JPromise2<Integer> ifHasAwait2(boolean input) {
        int a = 1;
        label:
        if (input) {
            break label;
        } else {
            a += JPromise2.just(1).await();
        }
        return JPromise2.just(a);
    }

    @Test
    public void testIfHasAwait2() throws InterruptedException {
        Assertions.assertEquals(1, ifHasAwait2(true).block());
        Assertions.assertEquals(2, ifHasAwait2(false).block());
    }

    @Async
    public JPromise2<Integer> ifHasAwait3(boolean input) {
        int a = 1;
        label:
        if (JPromise2.just(input).await()) {
            break label;
        } else {
            a += JPromise2.just(1).await();
        }
        return JPromise2.just(a);
    }

    @Test
    public void testIfHasAwait3() throws InterruptedException {
        Assertions.assertEquals(1, ifHasAwait3(true).block());
        Assertions.assertEquals(2, ifHasAwait3(false).block());
    }

    @Async
    public JPromise2<Integer> whileNoAwait1() {
        int a = 1;
        label:
        while (a < 3) {
            if (a == 2) {
                break;
            }
            ++a;
        }
        return JPromise2.just(a);
    }

    @Test
    public void testWhileNoAwait1() throws InterruptedException {
        Assertions.assertEquals(2, whileNoAwait1().block());
    }

    @Async
    public JPromise2<Integer> whileNoAwait2() {
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
        return JPromise2.just(a);
    }

    @Test
    public void testWhileNoAwait2() throws InterruptedException {
        Assertions.assertEquals(20, whileNoAwait2().block());
    }

    @Async
    public JPromise2<Integer> whileNoAwait3() {
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
        return JPromise2.just(a);
    }

    @Test
    public void testWhileNoAwait3() throws InterruptedException {
        Assertions.assertEquals(20, whileNoAwait3().block());
    }

    @Async
    public JPromise2<Integer> whileNoAwait4() {
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
        return JPromise2.just(a);
    }

    @Test
    public void testWhileNoAwait4() throws InterruptedException {
        Assertions.assertEquals(45, whileNoAwait4().block());
    }

    @Async
    public JPromise2<Integer> whileHasAwait1() {
        int a = 1;
        label:
        while (a < JPromise2.just(3).await()) {
            if (a == JPromise2.just(2).await()) {
                break;
            }
            ++a;
        }
        return JPromise2.just(a);
    }

    @Test
    public void testWhileHasAwait1() throws InterruptedException {
        Assertions.assertEquals(2, whileHasAwait1().block());
    }

    @Async
    public JPromise2<Integer> whileHasAwait2() {
        int a = 0;
        int i = 0;
        label:
        while (i++ < 10) {
            if (i % JPromise2.just(2).await() == 0) {
                continue;
            }
            int j = 0;
            while (j++ < JPromise2.just(10).await()) {
                if (JPromise2.just(j).await() == i) {
                    break;
                }
                ++a;
            }
        }
        return JPromise2.just(a);
    }

    @Test
    public void testWhileHasAwait2() throws InterruptedException {
        Assertions.assertEquals(20, whileHasAwait2().block());
    }

    @Async
    public JPromise2<Integer> whileHasAwait3() {
        int a = 0;
        int i = 0;
        label:
        while (i++ < JPromise2.just(10).await()) {
            if (i % JPromise2.just(2).await() == 0) {
                continue;
            }
            int j = 0;
            while (j++ < JPromise2.just(10).await()) {
                if (JPromise2.just(j).await().equals(JPromise2.just(i).await())) {
                    continue label;
                }
                ++a;
            }
        }
        return JPromise2.just(a);
    }

    @Test
    public void testWhileHasAwait3() throws InterruptedException {
        Assertions.assertEquals(20, whileHasAwait3().block());
    }

    @Async
    public JPromise2<Integer> whileHasAwait4() {
        int a = 0;
        int i = 0;
        label1:
        while (i++ < JPromise2.just(10).await()) {
            int j = JPromise2.just(0).await();
            label2:
            while (j++ < 10) {
                if (j == JPromise2.just(i).await()) {
                    continue label1;
                }
                int k = 0;
                int ten = JPromise2.just(10).await();
                label3:
                while (k++ < ten) {
                    if (k == JPromise2.just(j).await()) {
                        break label2;
                    }
                    ++a;
                }
            }
            j = JPromise2.just(0).await();
            label2:
            while (j++ < JPromise2.just(10).await()) {
                int k = 0;
                label3:
                while (k++ < 10) {
                    if (JPromise2.just(i).await().equals(JPromise2.just(k).await())) {
                        break label2;
                    }
                    ++a;
                }
            }
        }
        return JPromise2.just(a);
    }

    @Test
    public void testWhileHasAwait4() throws InterruptedException {
        Assertions.assertEquals(45, whileHasAwait4().block());
    }

    @Async
    public JPromise2<Integer> doWhileNoAwait1() {
        int a = 1;
        label:
        do {
            if (a == 2) {
                break;
            }
            ++a;
        }
        while (a < 3);
        return JPromise2.just(a);
    }

    @Test
    public void testDoWhileNoAwait1() throws InterruptedException {
        Assertions.assertEquals(2, doWhileNoAwait1().block());
    }

    @Async
    public JPromise2<Integer> doWhileNoAwait2() {
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
        return JPromise2.just(a);
    }

    @Test
    public void testDoWhileNoAwait2() throws InterruptedException {
        Assertions.assertEquals(25, doWhileNoAwait2().block());
    }

    @Async
    public JPromise2<Integer> doWhileNoAwait3() {
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
        return JPromise2.just(a);
    }

    @Test
    public void testDoWhileNoAwait3() throws InterruptedException {
        Assertions.assertEquals(25, doWhileNoAwait3().block());
    }

    @Async
    public JPromise2<Integer> doWhileNoAwait4() {
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
        return JPromise2.just(a);
    }

    @Test
    public void testDoWhileNoAwait4() throws InterruptedException {
        Assertions.assertEquals(78, doWhileNoAwait4().block());
    }

    @Async
    public JPromise2<Integer> doWhileHasAwait1() {
        int a = 1;
        label:
        do {
            if (a == JPromise2.just(2).await()) {
                break;
            }
            ++a;
        }
        while (a < JPromise2.just(3).await());
        return JPromise2.just(a);
    }

    @Test
    public void testDoWhileHasAwait1() throws InterruptedException {
        Assertions.assertEquals(2, doWhileHasAwait1().block());
    }

    @Async
    public JPromise2<Integer> doWhileHasAwait2() {
        int a = 0;
        int i = 0;
        label:
        do {
            if (i % JPromise2.just(2).await() == 0) {
                continue;
            }
            int j = 0;
            do {
                if (JPromise2.just(j).await() == i) {
                    break;
                }
                ++a;
            }
            while (j++ < JPromise2.just(10).await());
        }
        while (i++ < 10);
        return JPromise2.just(a);
    }

    @Test
    public void testDoWhileHasAwait2() throws InterruptedException {
        Assertions.assertEquals(25, doWhileHasAwait2().block());
    }

    @Async
    public JPromise2<Integer> doWhileHasAwait3() {
        int a = 0;
        int i = 0;
        label:
        do {
            if (i % JPromise2.just(2).await() == 0) {
                continue;
            }
            int j = 0;
            do {
                if (JPromise2.just(j).await().equals(JPromise2.just(i).await())) {
                    continue label;
                }
                ++a;
            }
            while (j++ < JPromise2.just(10).await());
        }
        while (i++ < JPromise2.just(10).await());
        return JPromise2.just(a);
    }

    @Test
    public void testDoWhileHasAwait3() throws InterruptedException {
        Assertions.assertEquals(25, doWhileHasAwait3().block());
    }

    @Async(debug = true)
    public JPromise2<Integer> doWhileHasAwait4() {
        int a = 0;
        int i = 0;
        label1:
        do {
            int j = JPromise2.just(0).await();
            label2:
            do {
                boolean test = JPromise2.just(i).await().equals(JPromise2.just(j).await());
                if (test) {
                    continue label1;
                }
                int k = JPromise2.just(0).await();
                label3:
                do {
                    if ((k + JPromise2.just(j).await()) % 5 == 4) {
                        break label2;
                    } else if ((JPromise2.just(k).await() + j) % 5 == 1) {
                        continue label2;
                    }
                    ++a;
                    ++k;
                }
                while (true);
            }
            while (j++ < JPromise2.just(10).await());
            j = 0;
            label2:
            do {
                int k = JPromise2.just(0).await();
                label3:
                do {
                    if (i == k) {
                        break label2;
                    }
                    ++a;
                }
                while (k++ < JPromise2.just(10).await());
            }
            while (j++ < JPromise2.just(10).await());
        }
        while (i++ < 10);
        return JPromise2.just(a);
    }

    @Test
    public void testDoWhileHasAwait4() throws InterruptedException {
        Assertions.assertEquals(78, doWhileHasAwait4().block());
    }

    @Async
    public JPromise2<Integer> forNoAwait1(int num) {
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
        return JPromise2.just(a);
    }

    @Test
    public void testForNoAwait1() throws InterruptedException {
        for (int i = 0; i < 10; ++i) {
            if (i <= 5) {
                Assertions.assertEquals(i / 2, forNoAwait1(i).block());
            } else {
                Assertions.assertEquals(2, forNoAwait1(i).block());
            }
        }
    }

    @Async
    public JPromise2<Integer> forNoAwait2(int num) {
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
        return JPromise2.just(a);
    }

    @Test
    public void testForNoAwait2() throws InterruptedException {
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

    @Async(debug = true)
    public JPromise2<Integer> forHasAwait1(int num) {
        int a = 0;
        label:
        for (int i = JPromise2.just(0).await(); i < JPromise2.just(num).await(); ++i) {
            if (JPromise2.just(i).await() % 2 == JPromise2.just(0).await()) {
                continue;
            }
            if (i == JPromise2.just(5).await()) {
                break;
            }
            ++a;
        }
        return JPromise2.just(a);
    }

    @Test
    public void testForHasAwait1() throws InterruptedException {
        for (int i = 0; i < 10; ++i) {
            if (i <= 5) {
                Assertions.assertEquals(i / 2, forHasAwait1(i).block());
            } else {
                Assertions.assertEquals(2, forHasAwait1(i).block());
            }
        }
    }

    @Async(debug = true)
    public JPromise2<Integer> forHasAwait2(int num) {
        int a = 0;
        label1:
        for (int i = 0; JPromise2.just(i).await() < num; ++i) {
            label2:
            for (int j = JPromise2.just(0).await(); j < JPromise2.just(num).await(); ++j) {
                if (i == JPromise2.just(j).await()) {
                    if (JPromise2.just(i).await() < JPromise2.just(5).await()) {
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
                if (i == JPromise2.just(j).await()) {
                    if (i < 5) {
                        continue label1;
                    } else if (JPromise2.just(i).await() < 7) {
                        continue;
                    } else if (i < JPromise2.just(9).await()) {
                        break label1;
                    } else {
                        break;
                    }
                }
                ++a;
            }
        }
        return JPromise2.just(a);
    }

    @Test
    public void testForHasAwait2() throws InterruptedException {
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

    @Async
    private JPromise2<Integer> foreachContinueNoAwait() {
        int[] array = new int[] {1, 2, 3, 4};
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        Set<Integer> set = new LinkedHashSet<>();
        set.add(1);
        set.add(2);
        set.add(3);
        set.add(4);
        set.add(5);
        int a = 4;
        label0:
        label1:
        for (int i : array) {
            label2:
            for (int j : list) {
                if (j == 3) {
                    continue label1;
                }
                label3:
                for (int k : set) {
                    if (k == 5) {
                        continue label2;
                    }
                    a += k;
                }
                a += j;
            }
            a += i;
        }
        return JPromise2.just(a);
    }

    @Async
    private JPromise2<Integer> foreachContinueHasAwait() {
        int[] array = new int[] {1, 2, 3, 4};
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        Set<Integer> set = new LinkedHashSet<>();
        set.add(1);
        set.add(2);
        set.add(3);
        set.add(4);
        set.add(5);
        int a = 4;
        label0:
        label1:
        for (int i : JPromise2.just(array).await()) {
            label2:
            for (int j : list) {
                if (JPromise2.just(j).await() == 3) {
                    continue label1;
                }
                label3:
                for (int k : JPromise2.just(set).await()) {
                    if (JPromise2.just(k).await().equals(JPromise2.just(5).await())) {
                        continue label2;
                    }
                    a += JPromise2.just(k).await();
                }
                a += JPromise2.just(j).await();
            }
            a += i;
        }
        return JPromise2.just(a);
    }

    @Test
    public void testForeach() throws InterruptedException {
        Assertions.assertEquals(foreachContinueNoAwait().block(), foreachContinueHasAwait().block());
    }
}
