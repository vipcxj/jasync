package io.github.vipcxj.jasync.spec;

import io.github.vipcxj.jasync.spec.functional.*;
import io.github.vipcxj.jasync.spec.switchexpr.EnumCase;
import io.github.vipcxj.jasync.spec.switchexpr.IntCase;
import io.github.vipcxj.jasync.spec.switchexpr.StringCase;

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
    Promise<T> doFinally(VoidPromiseSupplier block);
    Promise<T> doWhile(BooleanSupplier predicate, PromiseFunction<T, T> block);
    Promise<Void> doWhileVoid(BooleanSupplier predicate, VoidPromiseSupplier block);
    Promise<T> doWhile(PromiseSupplier<Boolean> predicate, PromiseFunction<T, T> block);
    Promise<Void> doWhileVoid(PromiseSupplier<Boolean> predicate, VoidPromiseSupplier block);
    Promise<T> doDoWhile(BooleanSupplier predicate, PromiseFunction<T, T> block);
    Promise<Void> doDoWhileVoid(BooleanSupplier predicate, VoidPromiseSupplier block);
    Promise<T> doDoWhile(PromiseSupplier<Boolean> predicate, PromiseFunction<T, T> block);
    Promise<Void> doDoWhileVoid(PromiseSupplier<Boolean> predicate, VoidPromiseSupplier block);

    <E> Promise<Void> doForEachIterable(Iterable<E> iterable, VoidPromiseFunction<E> block);
    <E> Promise<Void> doForEachObjectArray(E[] array, VoidPromiseFunction<E> block);
    Promise<Void> doForEachByteArray(byte[] array, ByteVoidPromiseFunction block);
    Promise<Void> doForEachCharArray(char[] array, CharVoidPromiseFunction block);
    Promise<Void> doForEachBooleanArray(boolean[] array, BooleanVoidPromiseFunction block);
    Promise<Void> doForEachShortArray(short[] array, ShortVoidPromiseFunction block);
    Promise<Void> doForEachIntArray(int[] array, IntVoidPromiseFunction block);
    Promise<Void> doForEachLongArray(long[] array, LongVoidPromiseFunction block);
    Promise<Void> doForEachFloatArray(float[] array, FloatVoidPromiseFunction block);
    Promise<Void> doForEachDoubleArray(double[] array, DoubleVoidPromiseFunction block);

    Promise<Void> doIntSwitch(int value, List<IntCase> cases, VoidPromiseSupplier defaultBody);
    default Promise<Void> doIntSwitch(int value, List<IntCase> cases) {
        return doIntSwitch(value, cases, null);
    }
    Promise<Void> doStringSwitch(String value, List<StringCase> cases, VoidPromiseSupplier defaultBody);
    default Promise<Void> doStringSwitch(String value, List<StringCase> cases) {
        return doStringSwitch(value, cases, null);
    }
    <E extends Enum<E>> Promise<Void> doEnumSwitch(Enum<E> value, List<EnumCase<E>> cases, VoidPromiseSupplier defaultBody);
    default  <E extends Enum<E>> Promise<Void> doEnumSwitch(Enum<E> value, List<EnumCase<E>> cases) {
        return doEnumSwitch(value, cases, null);
    }

    <O> Promise<O> catchReturn();
    Handle async();
    T block();
    T block(Duration duration);
    <I> I unwrap();
}
