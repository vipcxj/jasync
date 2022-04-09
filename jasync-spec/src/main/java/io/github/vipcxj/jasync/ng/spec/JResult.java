package io.github.vipcxj.jasync.ng.spec;

public interface JResult<T> {
    boolean isResolved();
    T getResolvedValue();
    Throwable getRejectedError();
}
