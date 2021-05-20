package io.github.vipcxj.asyncjava;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.*;

public interface Promise<T> {

    default T await() {
        throw new UnsupportedOperationException();
    }

    <O> Promise<O> then(Function<T, Promise<O>> resolver);
    default <O> Promise<O> then(Supplier<Promise<O>> resolver) {
        return this.then(ignored -> {
            return resolver.get();
        });
    }
    default Promise<Void> then(Consumer<T> resolver) {
        return this.then(v -> {
            resolver.accept(v);
            return null;
        });
    }
    Promise<T> doCatch(List<Class<? extends Throwable>> exceptionsType, Function<Throwable, Promise<T>> reject);
    default Promise<T> doCatch(List<Class<? extends Throwable>> exceptionsType, Supplier<Promise<T>> reject) {
        return this.doCatch(exceptionsType, ignored -> {
            return reject.get();
        });
    }
    default Promise<T> doCatch(List<Class<? extends Throwable>> exceptionsType, Consumer<Throwable> reject) {
        return this.doCatch(exceptionsType, t -> {
            reject.accept(t);
            return null;
        });
    }
    default Promise<T> doCatch(Function<Throwable, Promise<T>> reject) {
        return this.doCatch(Collections.singletonList(Throwable.class), reject);
    }
    default Promise<T> doCatch(Supplier<Promise<T>> reject) {
        return this.doCatch(Collections.singletonList(Throwable.class), ignored -> {
            return reject.get();
        });
    }
    default Promise<T> doCatch(Consumer<Throwable> reject) {
        return this.doCatch(Collections.singletonList(Throwable.class), t -> {
            reject.accept(t);
            return null;
        });
    }
    Promise<T> doFinally(Runnable block);
    Handle async();
    Promise<T> doWhile(BooleanSupplier predicate, Function<T, Promise<T>> block);
    Promise<Void> doWhile(BooleanSupplier predicate, Supplier<Promise<Void>> block);
    Promise<T> awaitWhile(Supplier<Promise<Boolean>> predicate, Function<T, Promise<T>> block);
    Promise<Void> awaitWhile(Supplier<Promise<Boolean>> predicate, Supplier<Promise<Void>> block);
    <O> Promise<O> doReturn();
    T block();
    T block(Duration duration);
    <I> I unwrap();

    static void doBreak() {
        throw new BreakException();
    }

    static void doReturn(Object v) {
        throw new ReturnException(v);
    }
}
