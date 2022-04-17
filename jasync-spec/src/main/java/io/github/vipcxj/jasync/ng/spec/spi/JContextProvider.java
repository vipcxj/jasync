package io.github.vipcxj.jasync.ng.spec.spi;

import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JPushContext;
import io.github.vipcxj.jasync.ng.spec.JScheduler;

public interface JContextProvider extends PrioritySupport {
    JContext defaultContext();
    JContext create(JScheduler scheduler);
    JPushContext createPushContext();
}
