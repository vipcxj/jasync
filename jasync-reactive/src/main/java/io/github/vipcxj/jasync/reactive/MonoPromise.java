package io.github.vipcxj.jasync.reactive;

import io.github.vipcxj.jasync.runtime.helpers.ArrayIterator;
import io.github.vipcxj.jasync.spec.*;
import io.github.vipcxj.jasync.spec.functional.*;
import io.github.vipcxj.jasync.spec.switchexpr.ICase;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MonoPromise<T> implements Promise<T> {
    private boolean resolved;
    private T value;
    private Throwable error;
    private final Mono<T> mono;

    public MonoPromise(Mono<T> mono) {
        this.mono = mono;
    }

    private void resolve(T value) {
        this.resolved = true;
        this.value = value;
        this.error = null;
    }

    private void reject(Throwable error) {
        this.resolved = true;
        this.value = null;
        this.error = error;
    }

    @Override
    public <O> Promise<O> then(PromiseFunction<T, O> resolver) {
        AtomicReference<Boolean> empty = new AtomicReference<>(true);
        return new MonoPromise<>(mono.<O>flatMap(v -> {
            resolve(v);
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
                resolve(null);
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
                            reject(t);
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

    private Mono<? extends AtomicReference<T>> doWhileBody(PromiseFunction<T, T> body, String label, AtomicReference<T> ref) throws Throwable {
        Promise<T> res = body.apply(ref.get());
        if (res != null) {
            return res.then(a -> {
                ref.set(a);
                return null;
            }).doCatch(ContinueException.class, e -> {
                if (e.matchLabel(label)) {
                    return null;
                }
                return JAsync.error(e);
            }).unwrap();
        } else {
            return Mono.empty();
        }
    }

    private Mono<? extends Integer> doWhileBody(VoidPromiseSupplier body, String label) throws Throwable {
        Promise<Void> res = body.get();
        if (res != null) {
            return res.then(a -> null).doCatch(ContinueException.class, e -> {
                if (e.matchLabel(label)) {
                    return null;
                }
                return JAsync.error(e);
            }).unwrap();
        } else {
            return Mono.empty();
        }
    }

    @Override
    public Promise<T> doWhile(BooleanSupplier predicate, PromiseFunction<T, T> block, String label) {
        return this.then(v -> {
            AtomicReference<T> ref = new AtomicReference<>(v);
            return new MonoPromise<>(Mono.defer(() -> {
                try {
                    if (predicate.getAsBoolean()) {
                        return doWhileBody(block, label, ref);
                    } else {
                        return Mono.just(ref);
                    }
                } catch (Throwable t) {
                    return Mono.error(t);
                }
            }).repeatWhenEmpty(i -> i).map(AtomicReference::get));
        }).doCatch(BreakException.class, e -> {
            if (e.matchLabel(label)) {
                return null;
            }
            return JAsync.error(e);
        });
    }

    @Override
    public Promise<Void> doWhileVoid(BooleanSupplier predicate, VoidPromiseSupplier block, String label) {
        return this.then(() -> new MonoPromise<Void>(Mono.defer(() -> {
            try {
                if (predicate.getAsBoolean()) {
                    return doWhileBody(block, label);
                } else {
                    return Mono.just(1);
                }
            } catch (Throwable t) {
                return Mono.error(t);
            }
        }).repeatWhenEmpty(i -> i).flatMap(v -> Mono.empty()))).doCatch(BreakException.class, e -> {
            if (e.matchLabel(label)) {
                return null;
            }
            return JAsync.error(e);
        });
    }

    @Override
    public Promise<T> doWhile(PromiseSupplier<Boolean> predicate, PromiseFunction<T, T> block, String label) {
        return this.then(v -> {
            AtomicReference<T> ref = new AtomicReference<>(v);
            return new MonoPromise<>(Mono.defer(() -> {
                try {
                    return predicate.get().<Mono<Boolean>>unwrap().flatMap(test -> {
                        try {
                            if (Boolean.TRUE.equals(test)) {
                                return doWhileBody(block, label, ref);
                            } else {
                                return Mono.just(ref);
                            }
                        } catch (Throwable t) {
                            return Mono.error(t);
                        }
                    });
                } catch (Throwable t) {
                    return Mono.error(t);
                }
            }).repeatWhenEmpty(i -> i).map(AtomicReference::get));
        }).doCatch(BreakException.class, e -> {
            if (e.matchLabel(label)) {
                return null;
            }
            return JAsync.error(e);
        });
    }

    @Override
    public Promise<Void> doWhileVoid(PromiseSupplier<Boolean> predicate, VoidPromiseSupplier block, String label) {
        return this.then(() -> new MonoPromise<Void>(Mono.defer(() -> {
            try {
                return predicate.get().<Mono<Boolean>>unwrap().flatMap(test -> {
                    try {
                        if (Boolean.TRUE.equals(test)) {
                            return doWhileBody(block, label);
                        } else {
                            return Mono.just(1);
                        }
                    } catch (Throwable t) {
                        return Mono.error(t);
                    }
                });
            } catch (Throwable t) {
                return Mono.error(t);
            }
        }).repeatWhenEmpty(i -> i).flatMap(v -> Mono.empty()))).doCatch(BreakException.class, e -> {
            if (e.matchLabel(label)) {
                return null;
            }
            return JAsync.error(e);
        });
    }

    private  <E> Promise<Void> doForEachIterator(Iterator<E> iterator, VoidPromiseFunction<E> block, String label) {
        return this.doWhileVoid(
                iterator::hasNext,
                () -> {
                    E next = iterator.next();
                    return block.apply(next);
                },
                label
        );
    }

    @Override
    public <E> Promise<Void> doForEachIterable(Iterable<E> iterable, VoidPromiseFunction<E> block, String label) {
        return doForEachIterator(iterable.iterator(), block, label);
    }

    @Override
    public <E> Promise<Void> doForEachObjectArray(E[] array, VoidPromiseFunction<E> block, String label) {
        return doForEachIterator(new ArrayIterator<>(array), block, label);
    }

    @Override
    public Promise<Void> doForEachBooleanArray(boolean[] array, BooleanVoidPromiseFunction block, String label) {
        AtomicInteger index = new AtomicInteger();
        int length = array.length;
        return this.doWhileVoid(
                () -> index.get() < length,
                () -> {
                    boolean next = array[index.getAndIncrement()];
                    return block.apply(next);
                },
                label
        );
    }

    @Override
    public Promise<Void> doForEachByteArray(byte[] array, ByteVoidPromiseFunction block, String label) {
        AtomicInteger index = new AtomicInteger();
        int length = array.length;
        return this.doWhileVoid(
                () -> index.get() < length,
                () -> {
                    byte next = array[index.getAndIncrement()];
                    return block.apply(next);
                },
                label
        );
    }

    @Override
    public Promise<Void> doForEachCharArray(char[] array, CharVoidPromiseFunction block, String label) {
        AtomicInteger index = new AtomicInteger();
        int length = array.length;
        return this.doWhileVoid(
                () -> index.get() < length,
                () -> {
                    char next = array[index.getAndIncrement()];
                    return block.apply(next);
                },
                label
        );
    }

    @Override
    public Promise<Void> doForEachShortArray(short[] array, ShortVoidPromiseFunction block, String label) {
        AtomicInteger index = new AtomicInteger();
        int length = array.length;
        return this.doWhileVoid(
                () -> index.get() < length,
                () -> {
                    short next = array[index.getAndIncrement()];
                    return block.apply(next);
                },
                label
        );
    }

    @Override
    public Promise<Void> doForEachIntArray(int[] array, IntVoidPromiseFunction block, String label) {
        AtomicInteger index = new AtomicInteger();
        int length = array.length;
        return this.doWhileVoid(
                () -> index.get() < length,
                () -> {
                    int next = array[index.getAndIncrement()];
                    return block.apply(next);
                },
                label
        );
    }

    @Override
    public Promise<Void> doForEachLongArray(long[] array, LongVoidPromiseFunction block, String label) {
        AtomicInteger index = new AtomicInteger();
        int length = array.length;
        return this.doWhileVoid(
                () -> index.get() < length,
                () -> {
                    long next = array[index.getAndIncrement()];
                    return block.apply(next);
                },
                label
        );
    }

    @Override
    public Promise<Void> doForEachFloatArray(float[] array, FloatVoidPromiseFunction block, String label) {
        AtomicInteger index = new AtomicInteger();
        int length = array.length;
        return this.doWhileVoid(
                () -> index.get() < length,
                () -> {
                    float next = array[index.getAndIncrement()];
                    return block.apply(next);
                },
                label
        );
    }

    @Override
    public Promise<Void> doForEachDoubleArray(double[] array, DoubleVoidPromiseFunction block, String label) {
        AtomicInteger index = new AtomicInteger();
        int length = array.length;
        return this.doWhileVoid(
                () -> index.get() < length,
                () -> {
                    double next = array[index.getAndIncrement()];
                    return block.apply(next);
                },
                label
        );
    }

    @Override
    public <C> Promise<Void> doSwitch(C value, List<? extends ICase<C>> cases, String label) {
        boolean matched = false;
        Promise<Void> result = null;
        for (int i = 0; i < 2; ++i) {
            for (ICase<C> aCase : cases) {
                if (!matched && aCase.is(value, i != 0)) {
                    matched = true;
                }
                if (matched) {
                    result = (result != null ? result : this).then(() -> {
                        try {
                            VoidPromiseSupplier body = aCase.getBody();
                            Promise<Void> promise = body != null ? body.get() : null;
                            return promise != null ? promise : JAsync.just();
                        } catch (Throwable t) {
                            return JAsync.error(t);
                        }
                    });
                }
            }
            if (matched) {
                break;
            }
        }
        return result != null ? result.doCatch(BreakException.class, e -> {
            if (e.matchLabel(label)) {
                return null;
            }
            return JAsync.error(e);
        }) : JAsync.just();
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
    public T await() {
        if (resolved) {
            if (error != null) {
                sneakyThrow(error);
            } else {
                return value;
            }
        }
        throw new UnsupportedOperationException();
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

    private static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        //noinspection unchecked
        throw (E) e;
    }
}
