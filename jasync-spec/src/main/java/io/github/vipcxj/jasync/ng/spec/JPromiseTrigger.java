package io.github.vipcxj.jasync.ng.spec;

public interface JPromiseTrigger<T> {

    JPromiseTrigger<T> start(JContext context);
    default JPromiseTrigger<T> start() {
        return start(JContext.defaultContext());
    }
    void resolve(T result);
    void reject(Throwable error);
    void cancel();
    JPromise<T> getPromise();
}
