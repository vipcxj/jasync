package io.github.vipcxj.jasync.ng.runtime.utils;

import java.util.Queue;

public interface AutoCapacityQueue<T> extends Queue<T> {

    int capacity();

    void clear(int maxCapacity);

    default void clear() {
        clear(256);
    }
}
