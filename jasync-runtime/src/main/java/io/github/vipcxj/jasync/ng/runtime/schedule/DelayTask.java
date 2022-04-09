package io.github.vipcxj.jasync.ng.runtime.schedule;

import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JThunk;

import java.util.concurrent.TimeUnit;

public class DelayTask implements Task<Void> {

    private final long timeout;
    private final TimeUnit unit;


    private volatile Thread thread;
    private volatile boolean canceled;

    public DelayTask(long timeout) {
        this(timeout, TimeUnit.MILLISECONDS);
    }

    public DelayTask(long timeout, TimeUnit unit) {
        this.timeout = timeout;
        this.unit = unit;
    }

    @Override
    public synchronized void schedule(JThunk<Void> thunk, JContext context) {
        if (!canceled) {
            thread = new Thread(() -> {
                try {
                    unit.sleep(timeout);
                    thunk.resolve(null, context);
                } catch (Throwable t) {
                    thunk.reject(t, context);
                }
            });
            thread.start();
        }
    }

    @Override
    public synchronized void cancel() {
        if (!canceled) {
            canceled = true;
            if (thread != null) {
                thread.interrupt();
            }
        }
    }
}
