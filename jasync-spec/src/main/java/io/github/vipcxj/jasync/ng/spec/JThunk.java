package io.github.vipcxj.jasync.ng.spec;

public interface JThunk<T> {

    void resolve(T result, JContext context);
    void reject(Throwable error, JContext context);
    default void interrupt(InterruptedException error, JContext context) {
        reject(error != null ? error : new InterruptedException(), context);
    }
    default void interrupt(JContext context) {
        interrupt(new InterruptedException(), context);
    }
    void onRequestCancel(Runnable runnable);
}
