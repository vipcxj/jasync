package io.github.vipcxj.jasync.ng.runtime.schedule;

import io.github.vipcxj.jasync.ng.spec.JDisposable;
import io.github.vipcxj.jasync.ng.spec.JScheduler;
import io.github.vipcxj.schedule.EventHandle;
import io.github.vipcxj.schedule.Schedule;

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
            final EventHandlerDisposable disposable = new EventHandlerDisposable();
            EventHandle handle = Schedule.instance().addEvent(delay, unit, () -> service.submit(task));
            disposable.updateHandle(handle);
            return disposable;
        }
    }

    private void setTimeout(Runnable task, long delay, TimeUnit unit, EventHandlerDisposable disposable) {
        this.service.submit(task);
        EventHandle handle = Schedule.instance().addEvent(delay, unit, () -> setTimeout(task, delay, unit, disposable));
        disposable.updateHandle(handle);
    }

    @Override
    public JDisposable schedulePeriodically(Runnable task, long initialDelay, long delay, TimeUnit unit) {
        if (service instanceof ScheduledExecutorService) {
            ScheduledExecutorService scheduledService = (ScheduledExecutorService) this.service;
            ScheduledFuture<?> future = scheduledService.scheduleWithFixedDelay(task, initialDelay, delay, unit);
            return new FutureDisposable<>(future);
        } else {
            final EventHandlerDisposable disposable = new EventHandlerDisposable();
            EventHandle handle = Schedule.instance().addEvent(initialDelay, unit, () -> {
                setTimeout(task, delay, unit, disposable);
            });
            disposable.updateHandle(handle);
            return disposable;
        }
    }

    @Override
    public boolean supportDelay() {
        return true;
    }

}
