package io.github.vipcxj.jasync.ng.spec;

public interface JPortal<T> {
    JPromise<T> jump();
    long repeated();
    boolean isInterrupted();
}
