package io.github.vipcxj.jasync.spec;

public interface JThunk<T> {

    void resolve(T result, JContext context);
    void reject(Throwable error, JContext context);
}
