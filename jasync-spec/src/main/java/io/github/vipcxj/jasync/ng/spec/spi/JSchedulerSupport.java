package io.github.vipcxj.jasync.ng.spec.spi;

import io.github.vipcxj.jasync.ng.spec.JScheduler;

import java.util.concurrent.Executor;

public interface JSchedulerSupport extends PrioritySupport {

    JScheduler defaultScheduler();
    JScheduler fromExecutorService(Executor executor);
}
