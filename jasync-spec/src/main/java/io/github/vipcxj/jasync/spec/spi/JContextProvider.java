package io.github.vipcxj.jasync.spec.spi;

import io.github.vipcxj.jasync.spec.JContext;
import io.github.vipcxj.jasync.spec.JPromise2;
import io.github.vipcxj.jasync.spec.JPushContext;

public interface JContextProvider extends PrioritySupport {
    JContext defaultContext();
    JPromise2<JContext> current();
    JPushContext createPushContext();
}
