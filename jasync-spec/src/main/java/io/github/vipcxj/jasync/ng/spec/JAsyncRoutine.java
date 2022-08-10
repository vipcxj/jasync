package io.github.vipcxj.jasync.ng.spec;

public interface JAsyncRoutine {
    long id();
    JAsyncRoutine fork();
    int getSharedLockCount();
    void incSharedLockCount();
    void decSharedLockCount();
}
