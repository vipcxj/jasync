package io.github.vipcxj.jasync.ng.spec;

import io.github.vipcxj.jasync.ng.spec.spi.JStreamSupport;

import java.util.function.Predicate;

public interface JStream<T> {

    JStreamSupport provider = Utils.getProvider(JStreamSupport.class);

    static <T> JStream<T> create() {
        return provider.create(0);
    }

    static <T> JStream<T> create(int capacity) {
        return provider.create(capacity);
    }

    JPromise<Void> push(T data);

    boolean tryPut(T data);

    JPromise<T> pop();

    T tryPop();

    JStream<T> pop(Predicate<T> filter);

    T tryPop(Predicate<T> filter);

    int getCapacity();

    default boolean isUnbound(){
        return getCapacity() <= 0;
    }
}
