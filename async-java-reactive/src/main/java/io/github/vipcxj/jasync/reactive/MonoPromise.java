package io.github.vipcxj.jasync.reactive;

import io.github.vipcxj.jasync.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

public class MonoPromise<T> implements Promise<T> {
    private final Mono<T> mono;

    public MonoPromise(Mono<T> mono) {
        this.mono = mono;
    }

    @Override
    public <O> Promise<O> then(Function<T, Promise<O>> resolver) {
        AtomicReference<Boolean> empty = new AtomicReference<>(true);
        return new MonoPromise<>(mono.<O>flatMap(v -> {
            empty.set(false);
            Promise<O> res = resolver.apply(v);
            return res != null ? res.unwrap() : Mono.empty();
        }).switchIfEmpty(Mono.defer(() -> {
            if (empty.get()) {
                Promise<O> res = resolver.apply(null);
                return res != null ? res.unwrap() : Mono.empty();
            } else {
                return Mono.empty();
            }
        })));
    }

    @Override
    public Promise<T> doCatch(List<Class<? extends Throwable>> exceptionsType, Function<Throwable, Promise<T>> reject) {
        return new MonoPromise<>(mono
                .onErrorResume(
                        t -> exceptionsType.stream().anyMatch(e -> e.isAssignableFrom(t.getClass())),
                        t -> {
                            Promise<T> res = reject.apply(t);
                            if (res == null) {
                                return Mono.empty();
                            } else {
                                return res.unwrap();
                            }
                        }
                ));
    }

    @Override
    public Promise<T> doFinally(Supplier<Promise<T>> block) {
        AtomicReference<Boolean> isCatch = new AtomicReference<>(false);
        return doCatch(t -> {
            isCatch.set(true);
            Promise<T> promise = block.get();
            promise = promise != null ? promise : just(null);
            return promise.then(() -> new MonoPromise<>(Mono.error(t)));
        }).then(v -> {
            if (!isCatch.get()) {
                Promise<T> promise = block.get();
                promise = promise != null ? promise : just(null);
                return promise.then(() -> just(v));
            } else {
                return just(v);
            }
        });
    }

    @Override
    public Promise<T> doWhile(BooleanSupplier predicate, Function<T, Promise<T>> block) {
        return this.then(v -> {
            if (predicate.getAsBoolean()) {
                return Promise.defer(() -> block.apply(v))
                        .doCatch(Collections.singletonList(ContinueException.class), e -> {})
                        .doWhile(predicate, block);
            } else {
                return null;
            }
        }).doCatch(Collections.singletonList(BreakException.class), e -> {});
    }

    @Override
    public Promise<Void> doWhileVoid(BooleanSupplier predicate, Supplier<Promise<Void>> block) {
        return this.then(() -> {
            if (predicate.getAsBoolean()) {
                return Promise.defer(block)
                        .doCatch(Collections.singletonList(ContinueException.class), e -> {})
                        .doWhileVoid(predicate, block);
            } else {
                return null;
            }
        }).doCatch(Collections.singletonList(BreakException.class), e -> {});
    }

    @Override
    public <O> Promise<O> doReturn() {
        AtomicBoolean retFlag = new AtomicBoolean(false);
        AtomicReference<O> retRef = new AtomicReference<>();
        return new MonoPromise<>(mono.onErrorResume(ReturnException.class, e -> {
            retFlag.set(true);
            //noinspection unchecked
            retRef.set((O) e.getValue());
            return Mono.empty();
        }).then(Mono.defer(() -> {
            if (retFlag.get()) {
                return Mono.justOrEmpty(retRef.get());
            } else {
                return Mono.empty();
            }
        })));
    }

    @Override
    public Handle async() {
        return new DisposableHandle(mono.subscribe());
    }

    @Override
    public T block() {
        return mono.block();
    }

    @Override
    public T block(Duration duration) {
        return mono.block(duration);
    }

    @Override
    public <I> I unwrap() {
        //noinspection unchecked
        return (I) mono;
    }

    public static <O> MonoPromise<O> just(O value) {
        return new MonoPromise<>(Mono.justOrEmpty(value));
    }

    public static MonoPromise<Void> just() {
        return just(null);
    }
}
