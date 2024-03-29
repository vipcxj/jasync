package io.github.vipcxj.jasync.ng.spec;

import io.github.vipcxj.jasync.ng.spec.exceptions.JAsyncExecutionException;
import io.github.vipcxj.jasync.ng.spec.spi.JSchedulerSupport;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public interface JScheduler {
    JSchedulerSupport provider = Utils.getProvider(JSchedulerSupport.class);
    static JScheduler defaultScheduler() {
        return provider.defaultScheduler();
    }
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
    static JScheduler fromExecutorService(Executor executor) {
        return provider.fromExecutorService(executor);
    }
}
