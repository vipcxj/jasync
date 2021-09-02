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
}
