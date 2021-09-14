package io.github.vipcxj.jasync.spec.catcher;

import io.github.vipcxj.jasync.spec.functional.PromiseFunction;

public class Catcher<E extends Throwable, T> {

    private final Class<E> exceptionType;
    private final PromiseFunction<E, T> reject;

    public Catcher(Class<E> exceptionType, PromiseFunction<E, T> function) {
        this.exceptionType = exceptionType;
        this.reject = function;
    }

    public boolean match(Throwable t) {
        return t != null && exceptionType.isAssignableFrom(t.getClass());
    }

    public Class<E> getExceptionType() {
        return exceptionType;
    }

    public PromiseFunction<E, T> getReject() {
        return reject;
    }

    public static <E extends Throwable, T> Catcher<E, T> of(Class<E> exceptionType, PromiseFunction<E, T> function) {
        return new Catcher<>(exceptionType, function);
    }
}
