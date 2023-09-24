package io.github.vipcxj.jasync.ng.runtime.stream;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JStream;

import java.util.function.Predicate;

public class BoundedStream<T> implements JStream<T> {

    private final int capacity;
    private final Object[] data;
    private int start;
    private int size;

    public BoundedStream(int capacity) {
        assert capacity > 0;
        this.capacity = capacity;
        this.data = new Object[this.capacity];
        this.start = 0;
        this.size = 0;
    }

    @Override
    public JPromise<Void> push(T data) {
        return null;
    }

    @Override
    public boolean tryPut(T data) {
        return false;
    }

    @Override
    public JPromise<T> pop() {
        return null;
    }

    @Override
    public T tryPop() {
        return null;
    }

    @Override
    public JStream<T> pop(Predicate<T> filter) {
        return null;
    }

    @Override
    public T tryPop(Predicate<T> filter) {
        return null;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }
}
