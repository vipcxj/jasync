package io.github.vipcxj.jasync.ng.test;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JPromiseTrigger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class PromiseTriggerTest {

    private JPromise<Integer> triggerSuccess(int expect) throws InterruptedException {
        JPromiseTrigger<Integer> trigger = JPromise.createTrigger();
        JPromise.sleep(300, TimeUnit.MILLISECONDS)
                .onFinally(() -> {
                    trigger.resolve(expect);
                }).await();
        return trigger.getPromise();
    }

    @Test
    public void testTriggerSuccess() throws InterruptedException {
        Assertions.assertEquals(123, triggerSuccess(123).block());
        Assertions.assertEquals(321, triggerSuccess(321).block());
        Assertions.assertEquals(132, triggerSuccess(132).block());
    }

    private JPromise<Integer> triggerError(Throwable t) throws InterruptedException {
        JPromiseTrigger<Integer> trigger = JPromise.createTrigger();
        JPromise.sleep(300, TimeUnit.MILLISECONDS)
                .onFinally(() -> {
                    trigger.reject(t);
                }).await();
        return trigger.getPromise();
    }

    @Test
    public void testTriggerError() {
        Utils.assertError(IOException.class, () -> triggerError(new IOException()).block());
        Utils.assertError(RuntimeException.class, () -> triggerError(new RuntimeException()).block());
    }

    private JPromise<Integer> triggerCancel() throws InterruptedException {
        JPromiseTrigger<Integer> trigger = JPromise.createTrigger();
        JPromise.sleep(300, TimeUnit.MILLISECONDS)
                .onFinally(trigger::cancel).await();
        return trigger.getPromise();
    }

    @Test
    public void testTriggerCancel() {
        Utils.assertError(InterruptedException.class, () -> {
            triggerCancel().block();
        });
    }
}
