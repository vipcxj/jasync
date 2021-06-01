package io.github.vipcxj.jasync.reactive;

import io.github.vipcxj.jasync.spec.*;
import io.github.vipcxj.jasync.spec.functional.BooleanSupplier;
import io.github.vipcxj.jasync.spec.functional.PromiseFunction;
import io.github.vipcxj.jasync.spec.functional.PromiseSupplier;
import io.github.vipcxj.jasync.spec.functional.VoidPromiseSupplier;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class MonoPromise<T> implements Promise<T> {
    private final Mono<T> mono;

    public MonoPromise(Mono<T> mono) {
        this.mono = mono;
    }

    @Override
    public <O> Promise<O> then(PromiseFunction<T, O> resolver) {
        AtomicReference<Boolean> empty = new AtomicReference<>(true);
        return new MonoPromise<>(mono.<O>flatMap(v -> {
            empty.set(false);
            Promise<O> res;
            try {
                res = resolver.apply(v);
            } catch (Throwable t) {
                return Mono.error(t);
            }
            return res != null ? res.unwrap() : Mono.empty();
        }).switchIfEmpty(Mono.defer(() -> {
            if (empty.get()) {
                Promise<O> res;
                try {
                    res = resolver.apply(null);
                } catch (Throwable t) {
                    return Mono.error(t);
                }
                return res != null ? res.unwrap() : Mono.empty();
            } else {
                return Mono.empty();
            }
        })));
    }

    @Override
    public Promise<T> doCatch(List<Class<? extends Throwable>> exceptionsType, PromiseFunction<Throwable, T> reject) {
        return new MonoPromise<>(mono
                .onErrorResume(
                        t -> exceptionsType.stream().anyMatch(e -> e.isAssignableFrom(t.getClass())),
                        t -> {
                            if (Promise.mustRethrowException(t, exceptionsType)) {
                                return Mono.error(t);
                            }
                            Promise<T> res;
                            try {
                                res = reject.apply(t);
                            } catch (Throwable e) {
                                return Mono.error(e);
                            }
                            if (res == null) {
                                return Mono.empty();
                            } else {
                                return res.unwrap();
                            }
                        }
                ));
    }

    @Override
    public Promise<T> doFinally(PromiseSupplier<T> block) {
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
    public Promise<T> doWhile(BooleanSupplier predicate, PromiseFunction<T, T> block) {
        return this.then(v -> {
            if (predicate.getAsBoolean()) {
                return Promise.defer(() -> block.apply(v))
                        .doCatch(Collections.singletonList(ContinueException.class), e -> {})
                        .doWhile(predicate, block);
            } else {
                return Promise.just(v);
            }
        }).doCatch(Collections.singletonList(BreakException.class), e -> {});
    }

    @Override
    public Promise<Void> doWhileVoid(BooleanSupplier predicate, VoidPromiseSupplier block) {
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
    public Promise<T> doWhile(PromiseSupplier<Boolean> predicate, PromiseFunction<T, T> block) {
        return this.then(v -> {
            Promise<Boolean> booleanPromise = predicate.get();
            if (booleanPromise == null) {
                return Promise.just(v);
            } else {
                return booleanPromise.then(test -> {
                    if (test != null && test) {
                        return Promise.defer(() -> block.apply(v))
                                .doCatch(Collections.singletonList(ContinueException.class), e -> {})
                                .doWhile(predicate, block);
                    } else {
                        return Promise.just(v);
                    }
                });
            }
        }).doCatch(Collections.singletonList(BreakException.class), e -> {});
    }

    @Override
    public Promise<Void> doWhileVoid(PromiseSupplier<Boolean> predicate, VoidPromiseSupplier block) {
        return this.then(() -> {
            Promise<Boolean> booleanPromise = predicate.get();
            if (booleanPromise == null) {
                return Promise.just();
            } else {
                return booleanPromise.then(test -> {
                    if (test != null && test) {
                        return Promise.defer(block)
                                .doCatch(Collections.singletonList(ContinueException.class), e -> {})
                                .doWhileVoid(predicate, block);
                    } else {
                        return Promise.just();
                    }
                });
            }
        }).doCatch(Collections.singletonList(BreakException.class), e -> {});
    }

    @Override
    public <O> Promise<O> catchReturn() {
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

}
