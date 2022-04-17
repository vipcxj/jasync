package io.github.vipcxj.jasync.ng.runtime.schedule;

import com.google.auto.service.AutoService;
import io.github.vipcxj.jasync.ng.spec.JScheduler;
import io.github.vipcxj.jasync.ng.spec.spi.JSchedulerSupport;

import java.util.concurrent.ExecutorService;

@AutoService(JSchedulerSupport.class)
public class SchedulerSupport implements JSchedulerSupport {
    @Override
    public JScheduler fromExecutorService(ExecutorService executorService) {
        return new ExecutorServiceScheduler(executorService);
    }
}
