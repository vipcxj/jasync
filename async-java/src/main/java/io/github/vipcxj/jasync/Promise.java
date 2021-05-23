package io.github.vipcxj.jasync;

import io.github.vipcxj.jasync.spi.PromiseProvider;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.*;

public interface Promise<T> {

    PromiseProvider provider = Utils.getProvider();

    default T await() {
        throw new UnsupportedOperationException();
    }

    <O> Promise<O> then(Function<T, Promise<O>> resolver);
    default <O> Promise<O> then(Supplier<Promise<O>> resolver) {
        return this.then(ignored -> resolver.get());
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
    Promise<T> doFinally(Supplier<Promise<T>> block);
    Promise<T> doWhile(BooleanSupplier predicate, Function<T, Promise<T>> block);
    Promise<Void> doWhileVoid(BooleanSupplier predicate, Supplier<Promise<Void>> block);
    <O> Promise<O> doReturn();
    Handle async();
    T block();
    T block(Duration duration);
    <I> I unwrap();

    static void doContinue() {
        throw new ContinueException();
    }

    static void doBreak() {
        throw new BreakException();
    }

    static <T, O> Promise<T> doReturn(Promise<O> promise) {
        if (promise != null) {
            return promise.then(v -> {
                throw new ReturnException(v);
            });
        } else {
            throw new ReturnException(null);
        }
    }

    static <T> Promise<T> just(T value) {
        if (provider == null) {
            throw new IllegalStateException("No provider of PromiseProvider found");
        }
        return provider.just(value);
    }

    static Promise<Void> just() {
        return just(null);
    }

    static  <T> Promise<T> defer(Supplier<Promise<T>> block) {
        if (provider == null) {
            throw new IllegalStateException("No provider of PromiseProvider found");
        }
        return provider.defer(block);
    }
}
