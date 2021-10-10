package io.github.vipcxj.jasync.spec.spi;

import io.github.vipcxj.jasync.spec.JContext;
import io.github.vipcxj.jasync.spec.JPromise2;

public interface JContextProvider extends PrioritySupport {
    JContext defaultContext();
    JPromise2<JContext> current();
}
