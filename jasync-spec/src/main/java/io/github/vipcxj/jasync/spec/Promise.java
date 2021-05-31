package io.github.vipcxj.jasync.spec;

import io.github.vipcxj.jasync.spec.spi.PromiseProvider;

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
    default Promise<Void> thenVoid(Function<T, Promise<Void>> resolver) {
        return this.then(resolver);
    }
    default Promise<Void> thenVoid(Supplier<Promise<Void>> resolver) {
        return this.then(resolver);
    }
    default Promise<Void> thenVoid() {
        return this.then(() -> null);
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
    default <E extends Throwable> Promise<T> doCatch(Class<E> exceptionType, Function<E, Promise<T>> reject) {
        return doCatch(Collections.singletonList(exceptionType), t -> {
            //noinspection unchecked
            return reject.apply((E) t);
        });
    }
    default <E extends Throwable> Promise<T> doCatch(Class<E> exceptionType, Supplier<Promise<T>> reject) {
        return this.doCatch(exceptionType, ignored -> {
            return reject.get();
        });
    }
    default <E extends Throwable> Promise<T> doCatch(Class<E> exceptionType, Consumer<E> reject) {
        return this.doCatch(exceptionType, t -> {
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
    Promise<T> doWhile(Supplier<Promise<Boolean>> predicate, Function<T, Promise<T>> block);
    Promise<Void> doWhileVoid(Supplier<Promise<Boolean>> predicate, Supplier<Promise<Void>> block);
    <O> Promise<O> catchReturn();
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

    static <T> Promise<T> defer(Supplier<Promise<T>> block) {
        if (provider == null) {
            throw new IllegalStateException("No provider of PromiseProvider found");
        }
        return provider.defer(block);
    }

    static Promise<Void> deferVoid(Supplier<Promise<Void>> block) {
        return defer(block);
    }

    static <T> Promise<T> error(Throwable t) {
        if (provider == null) {
            throw new IllegalStateException("No provider of PromiseProvider found");
        }
        return provider.error(t);
    }
}
