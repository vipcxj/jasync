package io.github.vipcxj.jasync.ng.spec;

import java.util.List;

public interface JHandle<T> {
    boolean isResolved();
    boolean isRejected();
    default boolean isCompleted() {
        return isResolved() || isRejected();
    }
    void cancel();
    boolean isCanceled();
    T block(JContext context) throws InterruptedException;
    default T block() throws InterruptedException {
        return block(JContext.defaultContext());
    }
    T getValue();
    Throwable getError();
    List<Throwable> getSuspendThrowables();
}
