package io.github.vipcxj.jasync.ng.runtime.stream;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JStream;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public class BoundedStream<T> implements JStream<T> {

    private final int capacity;
    private final Object[] data;
    private int start;
    private int size;
    private final List<BooleanSupplier> consumableCallbacks;
    private final List<BooleanSupplier> producableCallbacks;

    public BoundedStream(int capacity) {
        assert capacity > 0;
        this.capacity = capacity;
        this.data = new Object[this.capacity];
        this.start = 0;
        this.size = 0;
        this.consumableCallbacks = new ArrayList<>();
        this.producableCallbacks = new ArrayList<>();
    }

    private void triggerConsumableCallbacks() {
        consumableCallbacks.removeIf(BooleanSupplier::getAsBoolean);
    }

    private void triggerProducableCallbacks() {
        producableCallbacks.removeIf(BooleanSupplier::getAsBoolean);
    }

    @Override
    public JPromise<Void> produce(T data) {
        if (tryProduce(data)) {
            return JPromise.empty();
        } else {
            return JPromise.generate((thunk, context) -> {
                BooleanSupplier callback = () -> {
                    if (tryProduce(data)) {
                        thunk.resolve(null, context);
                        return true;
                    } else {
                        return false;
                    }
                };
                producableCallbacks.add(callback);
                thunk.onRequestCancel(() -> {
                    producableCallbacks.remove(callback);
                });
            });
        }
    }

    @Override
    public boolean tryProduce(T data) {
        return false;
    }

    @Override
    public JPromise<T> consume() {
        return consume(v -> true);
    }

    @Override
    public T tryConsume() {
        return tryConsume(v -> true);
    }

    @Override
    public JPromise<T> consume(Predicate<T> filter) {
        T v = tryConsume(filter);
        if (v != null) {
            return JPromise.just(v);
        } else {
            return JPromise.generate((thunk, context) -> {
                BooleanSupplier callback = () -> {
                    T v0 = tryConsume(filter);
                    if (v0 != null) {
                        thunk.resolve(v0, context);
                        return true;
                    } else {
                        return false;
                    }
                };
                consumableCallbacks.add(callback);
                thunk.onRequestCancel(() -> {
                    consumableCallbacks.remove(callback);
                });
            });
        }
    }

    @Override
    public T tryConsume(Predicate<T> filter) {
        return null;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }
}
