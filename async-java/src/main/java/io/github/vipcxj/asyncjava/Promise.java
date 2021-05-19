package io.github.vipcxj.asyncjava;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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
    <O> Promise<O> doCatch(Class<? extends Throwable>[] exceptionsType, Function<Throwable, Promise<O>> reject);
    <O> Promise<O> doCatch(Class<? extends Throwable>[] exceptionsType, Supplier<Promise<O>> reject);
    Promise<T> doCatch(Class<? extends Throwable>[] exceptionsType, Consumer<Throwable> reject);
    <O> Promise<O> doFinally(Supplier<Promise<O>> block);
    Promise<T> doFinally(Runnable block);
    <I> I unwrap(Class<I> type);
}
