package io.github.vipcxj.jasync.spec;

import io.github.vipcxj.jasync.spec.functional.*;
import io.github.vipcxj.jasync.spec.spi.PromiseProvider;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public interface Promise<T> {

    PromiseProvider provider = Utils.getProvider();

    default T await() {
        throw new UnsupportedOperationException();
    }

    <O> Promise<O> then(PromiseFunction<T, O> resolver);
    default <O> Promise<O> then(PromiseSupplier<O> resolver) {
        return this.then(ignored -> resolver.get());
    }
    default Promise<Void> thenVoid(VoidPromiseFunction<T> resolver) {
        return this.then(resolver);
    }
    default Promise<Void> thenVoid(VoidPromiseSupplier resolver) {
        return this.then(resolver);
    }
    default Promise<Void> thenVoid() {
        return this.then(() -> null);
    }
    Promise<T> doCatch(List<Class<? extends Throwable>> exceptionsType, PromiseFunction<Throwable, T> reject);
    default Promise<T> doCatch(List<Class<? extends Throwable>> exceptionsType, PromiseSupplier<T> reject) {
        return this.doCatch(exceptionsType, ignored -> {
            return reject.get();
        });
    }
    default Promise<T> doCatch(List<Class<? extends Throwable>> exceptionsType, ThrowableConsumer<Throwable> reject) {
        return this.doCatch(exceptionsType, t -> {
            reject.accept(t);
            return null;
        });
    }
    default <E extends Throwable> Promise<T> doCatch(Class<E> exceptionType, PromiseFunction<E, T> reject) {
        return doCatch(Collections.singletonList(exceptionType), t -> {
            //noinspection unchecked
            return reject.apply((E) t);
        });
    }
    default <E extends Throwable> Promise<T> doCatch(Class<E> exceptionType, PromiseSupplier<T> reject) {
        return this.doCatch(exceptionType, ignored -> {
            return reject.get();
        });
    }
    default <E extends Throwable> Promise<T> doCatch(Class<E> exceptionType, ThrowableConsumer<E> reject) {
        return this.doCatch(exceptionType, t -> {
            reject.accept(t);
            return null;
        });
    }
    default Promise<T> doCatch(PromiseFunction<Throwable, T> reject) {
        return this.doCatch(Collections.singletonList(Throwable.class), reject);
    }
    default Promise<T> doCatch(PromiseSupplier<T> reject) {
        return this.doCatch(Collections.singletonList(Throwable.class), ignored -> {
            return reject.get();
        });
    }
    default Promise<T> doCatch(ThrowableConsumer<Throwable> reject) {
        return this.doCatch(Collections.singletonList(Throwable.class), t -> {
            reject.accept(t);
            return null;
        });
    }
    Promise<T> doFinally(PromiseSupplier<T> block);
    Promise<T> doWhile(BooleanSupplier predicate, PromiseFunction<T, T> block);
    Promise<Void> doWhileVoid(BooleanSupplier predicate, VoidPromiseSupplier block);
    Promise<T> doWhile(PromiseSupplier<Boolean> predicate, PromiseFunction<T, T> block);
    Promise<Void> doWhileVoid(PromiseSupplier<Boolean> predicate, VoidPromiseSupplier block);
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

    static <T> Promise<T> defer(PromiseSupplier<T> block) {
        if (provider == null) {
            throw new IllegalStateException("No provider of PromiseProvider found");
        }
        return provider.defer(block);
    }

    static Promise<Void> deferVoid(VoidPromiseSupplier block) {
        return defer(block);
    }

    static <T> Promise<T> error(Throwable t) {
        if (provider == null) {
            throw new IllegalStateException("No provider of PromiseProvider found");
        }
        return provider.error(t);
    }

    static boolean mustRethrowException(Throwable t, List<Class<? extends Throwable>> exceptionsType) {
        if (t instanceof ReturnException) {
            return exceptionsType.stream().noneMatch(ReturnException.class::equals);
        }
        if (t instanceof BreakException) {
            return exceptionsType.stream().noneMatch(BreakException.class::equals);
        }
        if (t instanceof ContinueException) {
            return exceptionsType.stream().noneMatch(ContinueException.class::equals);
        }
        return false;
    }
}
