package io.github.vipcxj.jasync.ng.spec.spi;

import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JPushContext;

public interface JContextProvider extends PrioritySupport {
    JContext defaultContext();
    JPromise<JContext> current();
    JPushContext createPushContext();
}
