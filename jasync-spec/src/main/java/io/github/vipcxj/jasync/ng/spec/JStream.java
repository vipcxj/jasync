package io.github.vipcxj.jasync.ng.spec;

import io.github.vipcxj.jasync.ng.spec.spi.JStreamSupport;

import java.time.Duration;
import java.util.function.Predicate;

public interface JStream<T> {

    JStreamSupport provider = Utils.getProvider(JStreamSupport.class);

    static <T> JStream<T> create() {
        return provider.create(0);
    }

    static <T> JStream<T> create(int capacity) {
        return provider.create(capacity);
    }

    JPromise<Void> produce(T data);

    boolean tryProduce(T data);

    JPromise<T> consume();

    T tryConsume();

    JPromise<T> consume(Predicate<T> filter);

    T tryConsume(Predicate<T> filter);

    int getCapacity();

    default boolean isUnbound(){
        return getCapacity() <= 0;
    }
}
