package io.github.vipcxj.jasync.spec;

import io.github.vipcxj.jasync.spec.catcher.Catcher;
import io.github.vipcxj.jasync.spec.functional.*;
import io.github.vipcxj.jasync.spec.switchexpr.ICase;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

public interface JPromise<T> {

    default T await() {
        throw new UnsupportedOperationException();
    }

    <O> JPromise<O> then(PromiseFunction<T, O> resolver);
    default <O> JPromise<O> then(PromiseSupplier<O> resolver) {
        return this.then(ignored -> resolver.get());
    }
    default JPromise<Void> thenVoid(VoidPromiseFunction<T> resolver) {
        return this.then(resolver);
    }
    default JPromise<Void> thenVoid(VoidPromiseSupplier resolver) {
        return this.then(resolver);
    }
    default JPromise<Void> thenVoid() {
        return this.then(() -> null);
    }
    JPromise<T> doCatch(List<Class<? extends Throwable>> exceptionsType, PromiseFunction<Throwable, T> reject);
    default JPromise<T> doCatch(List<Class<? extends Throwable>> exceptionsType, PromiseSupplier<T> reject) {
        return this.doCatch(exceptionsType, ignored -> {
            return reject.get();
        });
    }
    default JPromise<T> doCatch(List<Class<? extends Throwable>> exceptionsType, ThrowableConsumer<Throwable> reject) {
        return this.doCatch(exceptionsType, t -> {
            reject.accept(t);
            return null;
        });
    }
    default <E extends Throwable> JPromise<T> doCatch(Class<E> exceptionType, PromiseFunction<E, T> reject) {
        return doCatch(Collections.singletonList(exceptionType), t -> {
            //noinspection unchecked
            return reject.apply((E) t);
        });
    }
    default <E extends Throwable> JPromise<T> doCatch(Class<E> exceptionType, PromiseSupplier<T> reject) {
        return this.doCatch(exceptionType, ignored -> {
            return reject.get();
        });
    }
    default <E extends Throwable> JPromise<T> doCatch(Class<E> exceptionType, ThrowableConsumer<E> reject) {
        return this.doCatch(exceptionType, t -> {
            reject.accept(t);
            return null;
        });
    }
    default JPromise<T> doCatch(PromiseFunction<Throwable, T> reject) {
        return this.doCatch(Collections.singletonList(Throwable.class), reject);
    }
    default JPromise<T> doCatch(PromiseSupplier<T> reject) {
        return this.doCatch(Collections.singletonList(Throwable.class), ignored -> {
            return reject.get();
        });
    }
    default JPromise<T> doCatch(ThrowableConsumer<Throwable> reject) {
        return this.doCatch(Collections.singletonList(Throwable.class), t -> {
            reject.accept(t);
            return null;
        });
    }
    JPromise<T> doCatch(List<Catcher<?, T>> catchers);
    JPromise<T> doFinally(VoidPromiseSupplier block);

    JPromise<T> doWhile(BooleanSupplier predicate, PromiseFunction<T, T> block, String label);
    JPromise<Void> doWhileVoid(BooleanSupplier predicate, VoidPromiseSupplier block, String label);
    JPromise<T> doWhile(PromiseSupplier<Boolean> predicate, PromiseFunction<T, T> block, String label);
    JPromise<Void> doWhileVoid(PromiseSupplier<Boolean> predicate, VoidPromiseSupplier block, String label);
    default JPromise<T> doDoWhile(BooleanSupplier predicate, PromiseFunction<T, T> block, String label) {
        return this.then(v -> Utils.safeApply(block, v).doCatch(ContinueException.class, e -> {
            if (e.matchLabel(label)) {
                return null;
            }
            return JAsync.error(e);
        }).doWhile(predicate, block, label));
    }
    default JPromise<Void> doDoWhileVoid(BooleanSupplier predicate, VoidPromiseSupplier block, String label) {
        return this.thenVoid(() -> Utils.safeGetVoid(block).doCatch(ContinueException.class, e -> {
            if (e.matchLabel(label)) {
                return null;
            }
            return JAsync.error(e);
        }).doWhileVoid(predicate, block, label));
    }
    default JPromise<T> doDoWhile(PromiseSupplier<Boolean> predicate, PromiseFunction<T, T> block, String label) {
        return this.then(v -> Utils.safeApply(block, v).doCatch(ContinueException.class, e -> {
            if (e.matchLabel(label)) {
                return null;
            }
            return JAsync.error(e);
        }).doWhile(predicate, block, label));
    }
    default JPromise<Void> doDoWhileVoid(PromiseSupplier<Boolean> predicate, VoidPromiseSupplier block, String label) {
        return this.thenVoid(() -> Utils.safeGetVoid(block).doCatch(ContinueException.class, e -> {
            if (e.matchLabel(label)) {
                return null;
            }
            return JAsync.error(e);
        }).doWhileVoid(predicate, block, label));
    }
    default JPromise<T> doWhile(BooleanSupplier predicate, PromiseFunction<T, T> block) {
        return doWhile(predicate, block, null);
    }
    default JPromise<Void> doWhileVoid(BooleanSupplier predicate, VoidPromiseSupplier block) {
        return doWhileVoid(predicate, block, null);
    }
    default JPromise<T> doWhile(PromiseSupplier<Boolean> predicate, PromiseFunction<T, T> block) {
        return doWhile(predicate, block, null);
    }
    default JPromise<Void> doWhileVoid(PromiseSupplier<Boolean> predicate, VoidPromiseSupplier block) {
        return doWhileVoid(predicate, block, null);
    }
    default JPromise<T> doDoWhile(BooleanSupplier predicate, PromiseFunction<T, T> block) {
        return doDoWhile(predicate, block, null);
    }
    default JPromise<Void> doDoWhileVoid(BooleanSupplier predicate, VoidPromiseSupplier block) {
        return doDoWhileVoid(predicate, block, null);
    }
    default JPromise<T> doDoWhile(PromiseSupplier<Boolean> predicate, PromiseFunction<T, T> block) {
        return doDoWhile(predicate, block, null);
    }
    default JPromise<Void> doDoWhileVoid(PromiseSupplier<Boolean> predicate, VoidPromiseSupplier block) {
        return doDoWhileVoid(predicate, block, null);
    }

    <E> JPromise<Void> doForEachIterable(Iterable<E> iterable, VoidPromiseFunction<E> block, String label);
    <E> JPromise<Void> doForEachObjectArray(E[] array, VoidPromiseFunction<E> block, String label);
    JPromise<Void> doForEachByteArray(byte[] array, ByteVoidPromiseFunction block, String label);
    JPromise<Void> doForEachCharArray(char[] array, CharVoidPromiseFunction block, String label);
    JPromise<Void> doForEachBooleanArray(boolean[] array, BooleanVoidPromiseFunction block, String label);
    JPromise<Void> doForEachShortArray(short[] array, ShortVoidPromiseFunction block, String label);
    JPromise<Void> doForEachIntArray(int[] array, IntVoidPromiseFunction block, String label);
    JPromise<Void> doForEachLongArray(long[] array, LongVoidPromiseFunction block, String label);
    JPromise<Void> doForEachFloatArray(float[] array, FloatVoidPromiseFunction block, String label);
    JPromise<Void> doForEachDoubleArray(double[] array, DoubleVoidPromiseFunction block, String label);
    default <E> JPromise<Void> doForEachIterable(Iterable<E> iterable, VoidPromiseFunction<E> block) {
        return doForEachIterable(iterable, block, null);
    }
    default <E> JPromise<Void> doForEachObjectArray(E[] array, VoidPromiseFunction<E> block) {
        return doForEachObjectArray(array, block, null);
    }
    default JPromise<Void> doForEachByteArray(byte[] array, ByteVoidPromiseFunction block) {
        return doForEachByteArray(array, block, null);
    }
    default JPromise<Void> doForEachCharArray(char[] array, CharVoidPromiseFunction block) {
        return doForEachCharArray(array, block, null);
    }
    default JPromise<Void> doForEachBooleanArray(boolean[] array, BooleanVoidPromiseFunction block) {
        return doForEachBooleanArray(array, block, null);
    }
    default JPromise<Void> doForEachShortArray(short[] array, ShortVoidPromiseFunction block) {
        return doForEachShortArray(array, block, null);
    }
    default JPromise<Void> doForEachIntArray(int[] array, IntVoidPromiseFunction block) {
        return doForEachIntArray(array, block, null);
    }
    default JPromise<Void> doForEachLongArray(long[] array, LongVoidPromiseFunction block) {
        return doForEachLongArray(array, block, null);
    }
    default JPromise<Void> doForEachFloatArray(float[] array, FloatVoidPromiseFunction block) {
        return doForEachFloatArray(array, block, null);
    }
    default JPromise<Void> doForEachDoubleArray(double[] array, DoubleVoidPromiseFunction block) {
        return doForEachDoubleArray(array, block, null);
    }

    <C> JPromise<Void> doSwitch(C value, List<? extends ICase<C>> cases, String label);
    default <C> JPromise<Void> doSwitch(C value, List<? extends ICase<C>> cases) {
        return doSwitch(value, cases, null);
    }

    <O> JPromise<O> catchReturn();
    Handle async();
    T block();
    T block(Duration duration);
    <I> I unwrap(Class<?> type);
}
