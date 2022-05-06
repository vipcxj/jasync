package io.github.vipcxj.jasync.ng.spec;

public interface JPromiseTrigger<T> {

    void resolve(T result);
    void reject(Throwable error);
    void cancel();
    JPromise<T> getPromise();
}
