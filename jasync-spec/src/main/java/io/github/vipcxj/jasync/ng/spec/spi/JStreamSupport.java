package io.github.vipcxj.jasync.ng.spec.spi;

import io.github.vipcxj.jasync.ng.spec.JStream;

public interface JStreamSupport extends PrioritySupport {

    <T> JStream<T> create(int capacity);
}
