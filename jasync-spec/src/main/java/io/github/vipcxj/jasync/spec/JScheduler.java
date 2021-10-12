package io.github.vipcxj.jasync.spec;

import io.github.vipcxj.jasync.spec.exceptions.JAsyncExecutionException;

import java.util.concurrent.TimeUnit;

public interface JScheduler {
    JDisposable schedule(Runnable task);
    default JDisposable schedule(Runnable task, long delay, TimeUnit unit) {
        throw new JAsyncExecutionException("Scheduler is not capable of time-based scheduling");
    }
    default JDisposable schedulePeriodically(Runnable task, long initialDelay, long delay, TimeUnit unit) {
        throw new JAsyncExecutionException("Scheduler is not capable of time-based scheduling");
    }
    default boolean supportDelay() {
        return false;
    }
}
