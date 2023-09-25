package io.github.vipcxj.jasync.ng.runtime.stream;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JStream;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public class BoundedStream<T> implements JStream<T> {

    private final int capacity;
    private final ConcurrentLinkedDeque<T> deque;
    private final AtomicInteger size;
    private final ConcurrentLinkedDeque<BooleanSupplier> consumableCallbacks;
    private final ConcurrentLinkedDeque<BooleanSupplier> producableCallbacks;

    public BoundedStream(int capacity) {
        this.capacity = capacity;
        this.deque = new ConcurrentLinkedDeque<>();
        this.size = new AtomicInteger(0);
        this.consumableCallbacks = new ConcurrentLinkedDeque<>();
        this.producableCallbacks = new ConcurrentLinkedDeque<>();
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
        int size = this.size.get();
        // size may greater than capacity
        if (isUnbound() || size < capacity) {
            deque.add(data);
            this.size.incrementAndGet();
            triggerConsumableCallbacks();
            return true;
        } else {
            return false;
        }
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
        if (deque.removeIf(filter)) {
            size.decrementAndGet();
            if (!isUnbound()) {
                triggerProducableCallbacks();
            }
        }
    }

    @Override
    public int getCapacity() {
        return capacity;
    }
}
