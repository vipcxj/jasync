package io.github.vipcxj.jasync.ng.runtime.schedule;

import io.github.vipcxj.jasync.ng.spec.JDisposable;
import io.github.vipcxj.jasync.ng.spec.JScheduler;
import io.github.vipcxj.jasync.ng.spec.exceptions.JAsyncExecutionException;

import java.util.concurrent.*;

public class ExecutorServiceScheduler implements JScheduler {

    private final ExecutorService service;

    public ExecutorServiceScheduler(ExecutorService service) {
        this.service = service;
    }

    @Override
    public JDisposable schedule(Runnable task) {
        Future<?> future = service.submit(task);
        return new FutureDisposable<>(future);
    }

    @Override
    public JDisposable schedule(Runnable task, long delay, TimeUnit unit) {
        if (service instanceof ScheduledExecutorService) {
            ScheduledExecutorService scheduledService = (ScheduledExecutorService) this.service;
            ScheduledFuture<?> future = scheduledService.schedule(task, delay, unit);
            return new FutureDisposable<>(future);
        } else {
            throw new JAsyncExecutionException("Scheduler is not capable of time-based scheduling");
        }
    }

    @Override
    public JDisposable schedulePeriodically(Runnable task, long initialDelay, long delay, TimeUnit unit) {
        if (service instanceof ScheduledExecutorService) {
            ScheduledExecutorService scheduledService = (ScheduledExecutorService) this.service;
            ScheduledFuture<?> future = scheduledService.scheduleWithFixedDelay(task, initialDelay, delay, unit);
            return new FutureDisposable<>(future);
        } else {
            throw new JAsyncExecutionException("Scheduler is not capable of periodically-time-based scheduling");
        }
    }

    @Override
    public boolean supportDelay() {
        return service instanceof ScheduledExecutorService;
    }
}
