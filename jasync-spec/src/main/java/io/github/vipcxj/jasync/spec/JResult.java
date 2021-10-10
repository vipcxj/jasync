package io.github.vipcxj.jasync.spec;

public interface JResult<T> {
    boolean isResolved();
    T getResolvedValue();
    Throwable getRejectedError();
}
