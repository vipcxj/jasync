package io.github.vipcxj.jasync.ng.runtime.concurrent;

import io.github.vipcxj.jasync.ng.spec.JAsyncRoutine;

public interface Waiter {

    void resume(boolean locked);
    void cancel();
    void reject(Throwable t);
    JAsyncRoutine getRoutine();
}
