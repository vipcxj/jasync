package io.github.vipcxj.jasync.reactive;

import io.github.vipcxj.jasync.runtime.helpers.ArrayIterator;
import io.github.vipcxj.jasync.spec.*;
import io.github.vipcxj.jasync.spec.catcher.Catcher;
import io.github.vipcxj.jasync.spec.functional.*;
import io.github.vipcxj.jasync.spec.switchexpr.ICase;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class ReactorPromise<T> implements JPromise<T> {
    private boolean resolved;
    private T value;
    private Throwable error;
    private final Mono<T> mono;

    public ReactorPromise(Mono<T> mono) {
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
    public <O> JPromise<O> then(PromiseFunction<T, O> resolver) {
        AtomicReference<Boolean> empty = new AtomicReference<>(true);
        return new ReactorPromise<>(mono.<O>flatMap(v -> {
            resolve(v);
            empty.set(false);
            try {
                return Utils.safeApply(resolver, v).unwrap(Mono.class);
            } catch (Throwable t) {
                return Mono.error(t);
            }
        }).switchIfEmpty(Mono.defer(() -> {
            if (empty.get()) {
                resolve(null);
                try {
                    return Utils.safeApply(resolver, null).unwrap(Mono.class);
                } catch (Throwable t) {
                    return Mono.error(t);
                }
            } else {
                return Mono.empty();
            }
        })));
    }

    @Override
    public JPromise<T> doCatch(List<Class<? extends Throwable>> exceptionsType, PromiseFunction<Throwable, T> reject) {
        return doCatch(exceptionsType, reject, true);
    }

    @Override
    public JPromise<T> doCatch(List<Catcher<?, T>> catchers) {
        return this.doCatch(Collections.singletonList(Throwable.class), t -> {
            List<? extends Class<? extends Throwable>> exceptionsType = catchers.stream().map(Catcher::getExceptionType).collect(Collectors.toList());
            if (JAsync.mustRethrowException(t, exceptionsType)) {
                return JAsync.error(t);
            }
            for (Catcher<?, T> catcher : catchers) {
                if (catcher.match(t)) {
                    //noinspection unchecked
                    PromiseFunction<Throwable, T> reject = (PromiseFunction<Throwable, T>) catcher.getReject();
                    JPromise<T> res = reject != null ? reject.apply(t) : null;
                    return res != null ? res : JAsync.just();
                }
            }
            return null;
        }, false);
    }

    private JPromise<T> doCatch(List<Class<? extends Throwable>> exceptionsType, PromiseFunction<Throwable, T> reject, boolean processInnerExceptions) {
        return new ReactorPromise<>(mono
                .onErrorResume(
                        t -> exceptionsType.stream().anyMatch(e -> e.isAssignableFrom(t.getClass())),
                        t -> {
                            if (processInnerExceptions && JAsync.mustRethrowException(t, exceptionsType)) {
                                return Mono.error(t);
                            }
                            reject(t);
                            try {
                                return Utils.safeApply(reject, t).unwrap(Mono.class);
                            } catch (Throwable e) {
                                return Mono.error(e);
                            }
                        }
                ));
    }

    @Override
    public JPromise<T> doFinally(VoidPromiseSupplier block) {
        AtomicReference<Boolean> isCatch = new AtomicReference<>(false);
        return doCatch(Collections.singletonList(Throwable.class), t -> {
            isCatch.set(true);
            return Utils.safeGetVoid(block).then(() -> JAsync.error(t));
        }, false).then(v -> {
            if (!isCatch.get()) {
                return Utils.safeGetVoid(block).then(() -> just(v));
            } else {
                return just(v);
            }
        });
    }

    private Mono<? extends AtomicReference<T>> doWhileBody(PromiseFunction<T, T> body, String label, AtomicReference<T> ref) throws Throwable {
        return Utils.safeApply(body, ref.get()).then(a -> {
            ref.set(a);
            return null;
        }).doCatch(ContinueException.class, e -> {
            if (e.matchLabel(label)) {
                return null;
            }
            return JAsync.error(e);
        }).unwrap(Mono.class);
    }

    private Mono<? extends Integer> doWhileBody(VoidPromiseSupplier body, String label) throws Throwable {
        return Utils.safeGetVoid(body).then(a -> null).doCatch(ContinueException.class, e -> {
            if (e.matchLabel(label)) {
                return null;
            }
            return JAsync.error(e);
        }).unwrap(Mono.class);
    }

    @Override
    public JPromise<T> doWhile(BooleanSupplier predicate, PromiseFunction<T, T> block, String label) {
        return this.then(v -> {
            AtomicReference<T> ref = new AtomicReference<>(v);
            return new ReactorPromise<>(Mono.defer(() -> {
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
    public JPromise<Void> doWhileVoid(BooleanSupplier predicate, VoidPromiseSupplier block, String label) {
        return this.then(() -> new ReactorPromise<Void>(Mono.defer(() -> {
            try {
                if (Utils.safeTest(predicate)) {
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
    public JPromise<T> doWhile(PromiseSupplier<Boolean> predicate, PromiseFunction<T, T> block, String label) {
        return this.then(v -> {
            AtomicReference<T> ref = new AtomicReference<>(v);
            return new ReactorPromise<>(Mono.defer(() -> {
                try {
                    return Utils.safeTest(predicate).<Mono<Boolean>>unwrap(Mono.class).flatMap(test -> {
                        try {
                            if (test) {
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
    public JPromise<Void> doWhileVoid(PromiseSupplier<Boolean> predicate, VoidPromiseSupplier block, String label) {
        return this.then(() -> new ReactorPromise<Void>(Mono.defer(() -> {
            try {
                return Utils.safeTest(predicate).<Mono<Boolean>>unwrap(Mono.class).flatMap(test -> {
                    try {
                        if (test) {
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

    private  <E> JPromise<Void> doForEachIterator(Iterator<E> iterator, VoidPromiseFunction<E> block, String label) {
        return this.doWhileVoid(
                iterator::hasNext,
                () -> {
                    E next = iterator.next();
                    return Utils.safeApply(block, next);
                },
                label
        );
    }

    @Override
    public <E> JPromise<Void> doForEachIterable(Iterable<E> iterable, VoidPromiseFunction<E> block, String label) {
        return doForEachIterator(iterable.iterator(), block, label);
    }

    @Override
    public <E> JPromise<Void> doForEachObjectArray(E[] array, VoidPromiseFunction<E> block, String label) {
        return doForEachIterator(new ArrayIterator<>(array), block, label);
    }

    @Override
    public JPromise<Void> doForEachBooleanArray(boolean[] array, BooleanVoidPromiseFunction block, String label) {
        AtomicInteger index = new AtomicInteger();
        int length = array.length;
        return this.doWhileVoid(
                () -> index.get() < length,
                () -> {
                    boolean next = array[index.getAndIncrement()];
                    return Utils.safeApply(block, next);
                },
                label
        );
    }

    @Override
    public JPromise<Void> doForEachByteArray(byte[] array, ByteVoidPromiseFunction block, String label) {
        AtomicInteger index = new AtomicInteger();
        int length = array.length;
        return this.doWhileVoid(
                () -> index.get() < length,
                () -> {
                    byte next = array[index.getAndIncrement()];
                    return Utils.safeApply(block, next);
                },
                label
        );
    }

    @Override
    public JPromise<Void> doForEachCharArray(char[] array, CharVoidPromiseFunction block, String label) {
        AtomicInteger index = new AtomicInteger();
        int length = array.length;
        return this.doWhileVoid(
                () -> index.get() < length,
                () -> {
                    char next = array[index.getAndIncrement()];
                    return Utils.safeApply(block, next);
                },
                label
        );
    }

    @Override
    public JPromise<Void> doForEachShortArray(short[] array, ShortVoidPromiseFunction block, String label) {
        AtomicInteger index = new AtomicInteger();
        int length = array.length;
        return this.doWhileVoid(
                () -> index.get() < length,
                () -> {
                    short next = array[index.getAndIncrement()];
                    return Utils.safeApply(block, next);
                },
                label
        );
    }

    @Override
    public JPromise<Void> doForEachIntArray(int[] array, IntVoidPromiseFunction block, String label) {
        AtomicInteger index = new AtomicInteger();
        int length = array.length;
        return this.doWhileVoid(
                () -> index.get() < length,
                () -> {
                    int next = array[index.getAndIncrement()];
                    return Utils.safeApply(block, next);
                },
                label
        );
    }

    @Override
    public JPromise<Void> doForEachLongArray(long[] array, LongVoidPromiseFunction block, String label) {
        AtomicInteger index = new AtomicInteger();
        int length = array.length;
        return this.doWhileVoid(
                () -> index.get() < length,
                () -> {
                    long next = array[index.getAndIncrement()];
                    return Utils.safeApply(block, next);
                },
                label
        );
    }

    @Override
    public JPromise<Void> doForEachFloatArray(float[] array, FloatVoidPromiseFunction block, String label) {
        AtomicInteger index = new AtomicInteger();
        int length = array.length;
        return this.doWhileVoid(
                () -> index.get() < length,
                () -> {
                    float next = array[index.getAndIncrement()];
                    return Utils.safeApply(block, next);
                },
                label
        );
    }

    @Override
    public JPromise<Void> doForEachDoubleArray(double[] array, DoubleVoidPromiseFunction block, String label) {
        AtomicInteger index = new AtomicInteger();
        int length = array.length;
        return this.doWhileVoid(
                () -> index.get() < length,
                () -> {
                    double next = array[index.getAndIncrement()];
                    return Utils.safeApply(block, next);
                },
                label
        );
    }

    @Override
    public <C> JPromise<Void> doSwitch(C value, List<? extends ICase<C>> cases, String label) {
        boolean matched = false;
        JPromise<Void> result = null;
        if (cases != null) {
            for (int i = 0; i < 2; ++i) {
                for (ICase<C> aCase : cases) {
                    if (!matched && aCase.is(value, i != 0)) {
                        matched = true;
                    }
                    if (matched) {
                        result = (result != null ? result : this).then(() -> {
                            try {
                                VoidPromiseSupplier body = aCase.getBody();
                                return Utils.safeGetVoid(body);
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
        }
        return result != null ? result.doCatch(BreakException.class, e -> {
            if (e.matchLabel(label)) {
                return null;
            }
            return JAsync.error(e);
        }) : JAsync.just();
    }

    @Override
    public <O> JPromise<O> catchReturn() {
        AtomicBoolean retFlag = new AtomicBoolean(false);
        AtomicReference<O> retRef = new AtomicReference<>();
        return new ReactorPromise<>(mono.onErrorResume(ReturnException.class, e -> {
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
    public <I> I unwrap(Class<?> type) {
        if (!Mono.class.equals(type)) {
            throw new UnwrapUnsupportedException(type, Mono.class);
        }
        //noinspection unchecked
        return (I) mono;
    }

    public static <O> ReactorPromise<O> just(O value) {
        return new ReactorPromise<>(Mono.justOrEmpty(value));
    }

    private static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        //noinspection unchecked
        throw (E) e;
    }
}
