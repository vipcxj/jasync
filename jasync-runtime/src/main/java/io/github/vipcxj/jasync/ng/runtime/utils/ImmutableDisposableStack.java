package io.github.vipcxj.jasync.ng.runtime.utils;

public class ImmutableDisposableStack<T> {

    private final T data;
    private final ImmutableDisposableStack<T> next;

    private ImmutableDisposableStack(T data, ImmutableDisposableStack<T> next) {
        this.data = data;
        this.next = next;
    }

    public static <T> ImmutableDisposableStack<T> create(T data) {
        return new ImmutableDisposableStack<>(data, null);
    }

    public static <T> ImmutableDisposableStack<T> create(T data1, T data2) {
        return new ImmutableDisposableStack<>(data1, create(data2));
    }

    public ImmutableDisposableStack<T> push(T data) {
        return new ImmutableDisposableStack<>(data, this);
    }

    public T top() {
        return data;
    }

    public ImmutableDisposableStack<T> pop() {
        return next;
    }
}
