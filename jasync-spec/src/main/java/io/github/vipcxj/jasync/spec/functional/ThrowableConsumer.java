package io.github.vipcxj.jasync.spec.functional;

public interface ThrowableConsumer<T extends Throwable> {

    void accept(T t) throws Throwable;
}
