package io.github.vipcxj.jasync.ng.spec.spi;

import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JScheduler;

public interface JContextProvider extends PrioritySupport {
    default JContext defaultContext() {
        return create(JScheduler.defaultScheduler(), true);
    }
    JContext create(JScheduler scheduler, boolean supportStackTrace);
}
