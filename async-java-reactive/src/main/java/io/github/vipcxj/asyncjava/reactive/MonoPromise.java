package io.github.vipcxj.asyncjava.reactive;

import io.github.vipcxj.asyncjava.BreakException;
import io.github.vipcxj.asyncjava.Handle;
import io.github.vipcxj.asyncjava.Promise;
import io.github.vipcxj.asyncjava.ReturnException;
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
    public Promise<T> doFinally(Runnable block) {
        AtomicReference<Boolean> isCatch = new AtomicReference<>(false);
        return doCatch(t -> {
            isCatch.set(true);
            block.run();
        }).then(v -> {
            if (!isCatch.get()) {
                block.run();
            }
            return new MonoPromise<>(Mono.justOrEmpty(v));
        });
    }

    @Override
    public Promise<T> doWhile(BooleanSupplier predicate, Function<T, Promise<T>> block) {
        return this.then(v -> {
            if (predicate.getAsBoolean()) {
                Promise<T> res = block.apply(v);
                res = res != null ? res : MonoPromise.just(null);
                return res.doWhile(predicate, block);
            } else {
                return null;
            }
        }).doCatch(Collections.singletonList(BreakException.class), e -> {});
    }

    @Override
    public Promise<Void> doWhile(BooleanSupplier predicate, Supplier<Promise<Void>> block) {
        return this.then(() -> {
            if (predicate.getAsBoolean()) {
                Promise<Void> res = block.get();
                res = res != null ? res : MonoPromise.just();
                return res.doWhile(predicate, block);
            } else {
                return null;
            }
        });
    }

    @Override
    public Promise<T> awaitWhile(Supplier<Promise<Boolean>> predicate, Function<T, Promise<T>> block) {
        return this.then(v -> {
            return predicate.get().then(test -> {
                if (test) {
                    Promise<T> res = block.apply(v);
                    res = res != null ? res : MonoPromise.just(null);
                    return res.awaitWhile(predicate, block);
                } else {
                    return null;
                }
            });
        });
    }

    @Override
    public Promise<Void> awaitWhile(Supplier<Promise<Boolean>> predicate, Supplier<Promise<Void>> block) {
        return this.then(v -> {
            return predicate.get().then(test -> {
                if (test) {
                    Promise<Void> res = block.get();
                    res = res != null ? res : MonoPromise.just();
                    return res.awaitWhile(predicate, block);
                } else {
                    return null;
                }
            });
        });
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
