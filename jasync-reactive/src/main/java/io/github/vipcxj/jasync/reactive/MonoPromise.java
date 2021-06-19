package io.github.vipcxj.jasync.reactive;

import io.github.vipcxj.jasync.spec.*;
import io.github.vipcxj.jasync.spec.functional.*;
import io.github.vipcxj.jasync.runtime.helpers.ArrayIterator;
import io.github.vipcxj.jasync.spec.switchexpr.Case;
import io.github.vipcxj.jasync.spec.switchexpr.EnumCase;
import io.github.vipcxj.jasync.spec.switchexpr.IntCase;
import io.github.vipcxj.jasync.spec.switchexpr.StringCase;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
                            if (JAsync.mustRethrowException(t, exceptionsType)) {
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
    public Promise<T> doFinally(VoidPromiseSupplier block) {
        AtomicReference<Boolean> isCatch = new AtomicReference<>(false);
        return doCatch(t -> {
            isCatch.set(true);
            Promise<Void> promise = block.get();
            promise = promise != null ? promise : just(null);
            return promise.then(() -> new MonoPromise<>(Mono.error(t)));
        }).then(v -> {
            if (!isCatch.get()) {
                Promise<Void> promise = block.get();
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
            AtomicReference<T> ref = new AtomicReference<>(v);
            return new MonoPromise<>(Mono.defer(() -> {
                try {
                    if (predicate.getAsBoolean()) {
                        return block.apply(ref.get()).then(a -> {
                            ref.set(a);
                            return null;
                        }).doCatch(Collections.singletonList(ContinueException.class), e -> null).unwrap();
                    } else {
                        return Mono.just(ref);
                    }
                } catch (Throwable t) {
                    return Mono.error(t);
                }
            }).repeatWhenEmpty(i -> i).map(AtomicReference::get));
        }).doCatch(Collections.singletonList(BreakException.class), e -> {});
    }

    @Override
    public Promise<Void> doWhileVoid(BooleanSupplier predicate, VoidPromiseSupplier block) {
        return this.then(() -> new MonoPromise<Void>(Mono.defer(() -> {
            try {
                if (predicate.getAsBoolean()) {
                    return block.get().then(a -> null).doCatch(Collections.singletonList(ContinueException.class), e -> null).unwrap();
                } else {
                    return Mono.just(1);
                }
            } catch (Throwable t) {
                return Mono.error(t);
            }
        }).repeatWhenEmpty(i -> i).flatMap(v -> Mono.empty()))).doCatch(Collections.singletonList(BreakException.class), e -> {});
    }

    @Override
    public Promise<T> doWhile(PromiseSupplier<Boolean> predicate, PromiseFunction<T, T> block) {
        return this.then(v -> {
            AtomicReference<T> ref = new AtomicReference<>(v);
            return new MonoPromise<>(predicate.get().then(test -> {
                try {
                    if (Boolean.TRUE.equals(test)) {
                        return block.apply(ref.get()).then(a -> {
                            ref.set(a);
                            return null;
                        }).doCatch(Collections.singletonList(ContinueException.class), e -> null).unwrap();
                    } else {
                        return JAsync.just(ref);
                    }
                } catch (Throwable t) {
                    return JAsync.error(t);
                }
            }).<Mono<AtomicReference<T>>>unwrap().repeatWhenEmpty(i -> i).map(AtomicReference::get));
        }).doCatch(Collections.singletonList(BreakException.class), e -> {});
    }

    @Override
    public Promise<Void> doWhileVoid(PromiseSupplier<Boolean> predicate, VoidPromiseSupplier block) {
        return this.then(v -> new MonoPromise<>(predicate.get().then(test -> {
            try {
                if (Boolean.TRUE.equals(test)) {
                    return block.get()
                            .then(() -> null)
                            .doCatch(Collections.singletonList(ContinueException.class), e -> null);
                } else {
                    return JAsync.just(1);
                }
            } catch (Throwable t) {
                return JAsync.error(t);
            }
        }).<Mono<Integer>>unwrap().repeatWhenEmpty(i -> i).<Void>flatMap(a -> Mono.empty()))).doCatch(Collections.singletonList(BreakException.class), e -> {});
    }

    @Override
    public Promise<T> doDoWhile(BooleanSupplier predicate, PromiseFunction<T, T> block) {
        return this.then(v -> block.apply(v).doWhile(predicate, block));
    }

    @Override
    public Promise<Void> doDoWhileVoid(BooleanSupplier predicate, VoidPromiseSupplier block) {
        return this.thenVoid(() -> block.get().doWhileVoid(predicate, block));
    }

    @Override
    public Promise<T> doDoWhile(PromiseSupplier<Boolean> predicate, PromiseFunction<T, T> block) {
        return this.then(v -> block.apply(v).doWhile(predicate, block));
    }

    @Override
    public Promise<Void> doDoWhileVoid(PromiseSupplier<Boolean> predicate, VoidPromiseSupplier block) {
        return this.thenVoid(() -> block.get().doWhileVoid(predicate, block));
    }

    private  <E> Promise<Void> doForEachIterator(Iterator<E> iterator, VoidPromiseFunction<E> block) {
        return this.doWhileVoid(
                iterator::hasNext,
                () -> {
                    E next = iterator.next();
                    return block.apply(next);
                }
        );
    }

    @Override
    public <E> Promise<Void> doForEachIterable(Iterable<E> iterable, VoidPromiseFunction<E> block) {
        return doForEachIterator(iterable.iterator(), block);
    }

    @Override
    public <E> Promise<Void> doForEachObjectArray(E[] array, VoidPromiseFunction<E> block) {
        return doForEachIterator(new ArrayIterator<>(array), block);
    }

    @Override
    public Promise<Void> doForEachBooleanArray(boolean[] array, BooleanVoidPromiseFunction block) {
        AtomicInteger index = new AtomicInteger();
        int length = array.length;
        return this.doWhileVoid(
                () -> index.get() < length,
                () -> {
                    boolean next = array[index.getAndIncrement()];
                    return block.apply(next);
                }
        );
    }

    @Override
    public Promise<Void> doForEachByteArray(byte[] array, ByteVoidPromiseFunction block) {
        AtomicInteger index = new AtomicInteger();
        int length = array.length;
        return this.doWhileVoid(
                () -> index.get() < length,
                () -> {
                    byte next = array[index.getAndIncrement()];
                    return block.apply(next);
                }
        );
    }

    @Override
    public Promise<Void> doForEachCharArray(char[] array, CharVoidPromiseFunction block) {
        AtomicInteger index = new AtomicInteger();
        int length = array.length;
        return this.doWhileVoid(
                () -> index.get() < length,
                () -> {
                    char next = array[index.getAndIncrement()];
                    return block.apply(next);
                }
        );
    }

    @Override
    public Promise<Void> doForEachShortArray(short[] array, ShortVoidPromiseFunction block) {
        AtomicInteger index = new AtomicInteger();
        int length = array.length;
        return this.doWhileVoid(
                () -> index.get() < length,
                () -> {
                    short next = array[index.getAndIncrement()];
                    return block.apply(next);
                }
        );
    }

    @Override
    public Promise<Void> doForEachIntArray(int[] array, IntVoidPromiseFunction block) {
        AtomicInteger index = new AtomicInteger();
        int length = array.length;
        return this.doWhileVoid(
                () -> index.get() < length,
                () -> {
                    int next = array[index.getAndIncrement()];
                    return block.apply(next);
                }
        );
    }

    @Override
    public Promise<Void> doForEachLongArray(long[] array, LongVoidPromiseFunction block) {
        AtomicInteger index = new AtomicInteger();
        int length = array.length;
        return this.doWhileVoid(
                () -> index.get() < length,
                () -> {
                    long next = array[index.getAndIncrement()];
                    return block.apply(next);
                }
        );
    }

    @Override
    public Promise<Void> doForEachFloatArray(float[] array, FloatVoidPromiseFunction block) {
        AtomicInteger index = new AtomicInteger();
        int length = array.length;
        return this.doWhileVoid(
                () -> index.get() < length,
                () -> {
                    float next = array[index.getAndIncrement()];
                    return block.apply(next);
                }
        );
    }

    @Override
    public Promise<Void> doForEachDoubleArray(double[] array, DoubleVoidPromiseFunction block) {
        AtomicInteger index = new AtomicInteger();
        int length = array.length;
        return this.doWhileVoid(
                () -> index.get() < length,
                () -> {
                    double next = array[index.getAndIncrement()];
                    return block.apply(next);
                }
        );
    }

    private <C> Promise<Void> doSwitch(C value, List<? extends Case<C>> cases, VoidPromiseSupplier defaultBody) {
        boolean matched = false;
        Mono<Void> result = null;
        for (Case<C> aCase : cases) {
            if (!matched && aCase.is(value)) {
                matched = true;
            }
            if (matched) {
                result = (result != null ? result : mono).then(Mono.defer(() -> {
                    try {
                        Promise<Void> promise = aCase.getBody().get();
                        return promise != null ? promise.unwrap() : Mono.empty();
                    } catch (Throwable t) {
                        return Mono.error(t);
                    }
                }));
            }
        }
        if (!matched && defaultBody != null) {
            result = mono.then(Mono.defer(() -> {
                try {
                    Promise<Void> promise = defaultBody.get();
                    return promise != null ? promise.unwrap() : Mono.empty();
                } catch (Throwable t) {
                    return Mono.error(t);
                }
            }));
        }
        return result != null ? new MonoPromise<>(result).doCatch(Collections.singletonList(BreakException.class), e -> {}) : new MonoPromise<>(Mono.empty());
    }

    @Override
    public Promise<Void> doIntSwitch(int value, List<IntCase> cases, VoidPromiseSupplier defaultBody) {
        return doSwitch(value, cases, defaultBody);
    }

    @Override
    public Promise<Void> doStringSwitch(String value, List<StringCase> cases, VoidPromiseSupplier defaultBody) {
        return doSwitch(value, cases, defaultBody);
    }

    @Override
    public <E extends Enum<E>> Promise<Void> doEnumSwitch(Enum<E> value, List<EnumCase<E>> enumCases, VoidPromiseSupplier defaultBody) {
        return doSwitch(value, enumCases, defaultBody);
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
