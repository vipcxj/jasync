package io.github.vipcxj.jasync.spec;

import io.github.vipcxj.jasync.spec.catcher.Catcher;
import io.github.vipcxj.jasync.spec.functional.*;
import io.github.vipcxj.jasync.spec.switchexpr.ICase;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

public interface Promise<T> {

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
    Promise<T> doCatch(List<Catcher<?, T>> catchers);
    Promise<T> doFinally(VoidPromiseSupplier block);

    Promise<T> doWhile(BooleanSupplier predicate, PromiseFunction<T, T> block, String label);
    Promise<Void> doWhileVoid(BooleanSupplier predicate, VoidPromiseSupplier block, String label);
    Promise<T> doWhile(PromiseSupplier<Boolean> predicate, PromiseFunction<T, T> block, String label);
    Promise<Void> doWhileVoid(PromiseSupplier<Boolean> predicate, VoidPromiseSupplier block, String label);
    default Promise<T> doDoWhile(BooleanSupplier predicate, PromiseFunction<T, T> block, String label) {
        return this.then(v -> Utils.safeApply(block, v).doCatch(ContinueException.class, e -> {
            if (e.matchLabel(label)) {
                return null;
            }
            return JAsync.error(e);
        }).doWhile(predicate, block, label));
    }
    default Promise<Void> doDoWhileVoid(BooleanSupplier predicate, VoidPromiseSupplier block, String label) {
        return this.thenVoid(() -> Utils.safeGetVoid(block).doCatch(ContinueException.class, e -> {
            if (e.matchLabel(label)) {
                return null;
            }
            return JAsync.error(e);
        }).doWhileVoid(predicate, block, label));
    }
    default Promise<T> doDoWhile(PromiseSupplier<Boolean> predicate, PromiseFunction<T, T> block, String label) {
        return this.then(v -> Utils.safeApply(block, v).doCatch(ContinueException.class, e -> {
            if (e.matchLabel(label)) {
                return null;
            }
            return JAsync.error(e);
        }).doWhile(predicate, block, label));
    }
    default Promise<Void> doDoWhileVoid(PromiseSupplier<Boolean> predicate, VoidPromiseSupplier block, String label) {
        return this.thenVoid(() -> Utils.safeGetVoid(block).doCatch(ContinueException.class, e -> {
            if (e.matchLabel(label)) {
                return null;
            }
            return JAsync.error(e);
        }).doWhileVoid(predicate, block, label));
    }
    default Promise<T> doWhile(BooleanSupplier predicate, PromiseFunction<T, T> block) {
        return doWhile(predicate, block, null);
    }
    default Promise<Void> doWhileVoid(BooleanSupplier predicate, VoidPromiseSupplier block) {
        return doWhileVoid(predicate, block, null);
    }
    default Promise<T> doWhile(PromiseSupplier<Boolean> predicate, PromiseFunction<T, T> block) {
        return doWhile(predicate, block, null);
    }
    default Promise<Void> doWhileVoid(PromiseSupplier<Boolean> predicate, VoidPromiseSupplier block) {
        return doWhileVoid(predicate, block, null);
    }
    default Promise<T> doDoWhile(BooleanSupplier predicate, PromiseFunction<T, T> block) {
        return doDoWhile(predicate, block, null);
    }
    default Promise<Void> doDoWhileVoid(BooleanSupplier predicate, VoidPromiseSupplier block) {
        return doDoWhileVoid(predicate, block, null);
    }
    default Promise<T> doDoWhile(PromiseSupplier<Boolean> predicate, PromiseFunction<T, T> block) {
        return doDoWhile(predicate, block, null);
    }
    default Promise<Void> doDoWhileVoid(PromiseSupplier<Boolean> predicate, VoidPromiseSupplier block) {
        return doDoWhileVoid(predicate, block, null);
    }

    <E> Promise<Void> doForEachIterable(Iterable<E> iterable, VoidPromiseFunction<E> block, String label);
    <E> Promise<Void> doForEachObjectArray(E[] array, VoidPromiseFunction<E> block, String label);
    Promise<Void> doForEachByteArray(byte[] array, ByteVoidPromiseFunction block, String label);
    Promise<Void> doForEachCharArray(char[] array, CharVoidPromiseFunction block, String label);
    Promise<Void> doForEachBooleanArray(boolean[] array, BooleanVoidPromiseFunction block, String label);
    Promise<Void> doForEachShortArray(short[] array, ShortVoidPromiseFunction block, String label);
    Promise<Void> doForEachIntArray(int[] array, IntVoidPromiseFunction block, String label);
    Promise<Void> doForEachLongArray(long[] array, LongVoidPromiseFunction block, String label);
    Promise<Void> doForEachFloatArray(float[] array, FloatVoidPromiseFunction block, String label);
    Promise<Void> doForEachDoubleArray(double[] array, DoubleVoidPromiseFunction block, String label);
    default <E> Promise<Void> doForEachIterable(Iterable<E> iterable, VoidPromiseFunction<E> block) {
        return doForEachIterable(iterable, block, null);
    }
    default <E> Promise<Void> doForEachObjectArray(E[] array, VoidPromiseFunction<E> block) {
        return doForEachObjectArray(array, block, null);
    }
    default Promise<Void> doForEachByteArray(byte[] array, ByteVoidPromiseFunction block) {
        return doForEachByteArray(array, block, null);
    }
    default Promise<Void> doForEachCharArray(char[] array, CharVoidPromiseFunction block) {
        return doForEachCharArray(array, block, null);
    }
    default Promise<Void> doForEachBooleanArray(boolean[] array, BooleanVoidPromiseFunction block) {
        return doForEachBooleanArray(array, block, null);
    }
    default Promise<Void> doForEachShortArray(short[] array, ShortVoidPromiseFunction block) {
        return doForEachShortArray(array, block, null);
    }
    default Promise<Void> doForEachIntArray(int[] array, IntVoidPromiseFunction block) {
        return doForEachIntArray(array, block, null);
    }
    default Promise<Void> doForEachLongArray(long[] array, LongVoidPromiseFunction block) {
        return doForEachLongArray(array, block, null);
    }
    default Promise<Void> doForEachFloatArray(float[] array, FloatVoidPromiseFunction block) {
        return doForEachFloatArray(array, block, null);
    }
    default Promise<Void> doForEachDoubleArray(double[] array, DoubleVoidPromiseFunction block) {
        return doForEachDoubleArray(array, block, null);
    }

    <C> Promise<Void> doSwitch(C value, List<? extends ICase<C>> cases, String label);
    default <C> Promise<Void> doSwitch(C value, List<? extends ICase<C>> cases) {
        return doSwitch(value, cases, null);
    }

    <O> Promise<O> catchReturn();
    Handle async();
    T block();
    T block(Duration duration);
    <I> I unwrap(Class<?> type);
}
