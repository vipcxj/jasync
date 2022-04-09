package io.github.vipcxj.jasync.ng.spec.spi;

public interface PrioritySupport {
    int PRIORITY_MIN = Integer.MIN_VALUE;
    int DEFAULT_VALUE = 0;
    default int priority() {
        return DEFAULT_VALUE;
    }
}
