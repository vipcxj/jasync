package io.github.vipcxj.jasync.ng.test;

import io.github.vipcxj.jasync.ng.spec.JHandle;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

public class ForTest {

    /**
     * it will loop forever, so just test compile.
     * @return not important.
     */
    @SuppressWarnings({"StatementWithEmptyBody", "InfiniteLoopStatement", "unused"})
    public JPromise<Void> emptyForLoop() {
        for (;;) { }
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public JPromise<Void> interruptableForLoop() {
        for (;;) {
            JPromise.sleep(1, TimeUnit.SECONDS).await();
        }
    }

    @Test
    public void testInterruptableForLoop() throws InterruptedException {
        JHandle<Void> handle = interruptableForLoop().async();
        handle.cancel();
    }
}
