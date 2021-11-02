package io.github.vipcxj.jasync.spec.catcher;

import io.github.vipcxj.jasync.spec.functional.JAsyncCatchFunction1;

public class Catcher2<T extends Throwable, R> {
    private final Class<T> exceptionType;
    private final JAsyncCatchFunction1<T, R> reject;

    public Catcher2(Class<T> exceptionType, JAsyncCatchFunction1<T, R> reject) {
        this.exceptionType = exceptionType;
        this.reject = reject;
    }

    public boolean match(Throwable t) {
        return t != null && exceptionType.isAssignableFrom(t.getClass());
    }

    public Class<T> getExceptionType() {
        return exceptionType;
    }

    public JAsyncCatchFunction1<T, R> getReject() {
        return reject;
    }

    public static <E extends Throwable, T> Catcher2<E, T> of(Class<E> exceptionType, JAsyncCatchFunction1<E, T> function) {
        return new Catcher2<>(exceptionType, function);
    }
}
