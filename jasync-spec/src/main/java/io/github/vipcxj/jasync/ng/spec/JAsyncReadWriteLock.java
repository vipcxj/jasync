package io.github.vipcxj.jasync.ng.spec;

public interface JAsyncReadWriteLock {
    JAsyncLock readLock();
    JAsyncLock writeLock();
}
