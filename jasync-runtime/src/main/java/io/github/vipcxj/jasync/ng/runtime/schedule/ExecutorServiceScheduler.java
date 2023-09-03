package io.github.vipcxj.jasync.ng.runtime.schedule;

import io.github.vipcxj.jasync.ng.spec.JDisposable;
import io.github.vipcxj.jasync.ng.spec.JScheduler;
import io.github.vipcxj.schedule.EventHandle;
import io.github.vipcxj.schedule.Schedule;

import java.util.concurrent.*;

public class ExecutorServiceScheduler implements JScheduler {

    private final Executor executor;

    public ExecutorServiceScheduler(Executor executor) {
        this.executor = executor;
    }

    @Override
    public JDisposable schedule(Runnable task) {
        if (executor instanceof ExecutorService) {
            Future<?> future = ((ExecutorService) executor).submit(task);
            return new FutureDisposable<>(future);
        } else {
            RunnableWrapper wrapper = new RunnableWrapper(task);
            executor.execute(wrapper);
            return wrapper;
        }
    }

    @Override
    public JDisposable schedule(Runnable task, long delay, TimeUnit unit) {
        if (executor instanceof ScheduledExecutorService) {
            ScheduledExecutorService scheduledService = (ScheduledExecutorService) this.executor;
            ScheduledFuture<?> future = scheduledService.schedule(task, delay, unit);
            return new FutureDisposable<>(future);
        } else {
            DisposableHandler disposableHandler = new DisposableHandler();
            final EventHandlerDisposable disposable = new EventHandlerDisposable(disposableHandler);
            EventHandle handle = Schedule.instance().addEvent(delay, unit, () -> {
                disposableHandler.updateDisposable(schedule(task));
            });
            disposable.updateHandle(handle);
            return disposable;
        }
    }

    private void setTimeout(Runnable task, long delay, TimeUnit unit, DisposableHandler disposableHandler, EventHandlerDisposable disposable) {
        disposableHandler.updateDisposable(schedule(task));
        EventHandle handle = Schedule.instance().addEvent(delay, unit, () -> setTimeout(task, delay, unit, disposableHandler, disposable));
        disposable.updateHandle(handle);
    }

    @Override
    public JDisposable schedulePeriodically(Runnable task, long initialDelay, long delay, TimeUnit unit) {
        if (executor instanceof ScheduledExecutorService) {
            ScheduledExecutorService scheduledService = (ScheduledExecutorService) this.executor;
            ScheduledFuture<?> future = scheduledService.scheduleWithFixedDelay(task, initialDelay, delay, unit);
            return new FutureDisposable<>(future);
        } else {
            DisposableHandler disposableHandler = new DisposableHandler();
            final EventHandlerDisposable disposable = new EventHandlerDisposable(disposableHandler);
            EventHandle handle = Schedule.instance().addEvent(initialDelay, unit, () -> {
                setTimeout(task, delay, unit, disposableHandler, disposable);
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
