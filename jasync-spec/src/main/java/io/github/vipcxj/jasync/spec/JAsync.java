package io.github.vipcxj.jasync.spec;

import io.github.vipcxj.jasync.spec.functional.*;
import io.github.vipcxj.jasync.spec.spi.PromiseProvider;
import io.github.vipcxj.jasync.spec.switchexpr.ICase;

import java.util.List;

public class JAsync {

    private static final PromiseProvider provider = Utils.getProvider();

    private static PromiseProvider assertProvider() {
        if (provider == null) {
            throw new IllegalStateException("No provider of PromiseProvider found!");
        }
        return provider;
    }

    public static <T> Promise<T> just() {
        return assertProvider().just(null);
    }

    public static <T> Promise<T> just(T value) {
        return assertProvider().just(value);
    }

    public static <T> Promise<T> defer(PromiseSupplier<T> block) {
        return assertProvider().defer(block);
    }

    public static Promise<Void> deferVoid(VoidPromiseSupplier block) {
        return defer(block);
    }

    public static <T> Promise<T> error(Throwable t) {
        return assertProvider().error(t);
    }

    public static void doContinue(String label) {
        throw new ContinueException(label);
    }

    public static void doBreak(String label) {
        throw new BreakException(label);
    }

    public static <T, O> Promise<T> doReturn(Promise<O> promise) {
        if (promise != null) {
            return promise.then(v -> error(new ReturnException(v)));
        } else {
            return error(new ReturnException(null));
        }
    }

    public static <C> Promise<Void> doSwitch(C value, List<? extends ICase<C>> cases) {
        return just().doSwitch(value, cases);
    }

    public static Promise<Void> doWhile(BooleanSupplier predicate, VoidPromiseSupplier block) {
        return just().doWhileVoid(predicate, block);
    }

    public static Promise<Void> doWhile(PromiseSupplier<Boolean> predicate, VoidPromiseSupplier block) {
        return just().doWhileVoid(predicate, block);
    }

    public static Promise<Void> doDoWhile(BooleanSupplier predicate, VoidPromiseSupplier block) {
        return just().doDoWhileVoid(predicate, block);
    }

    public static Promise<Void> doDoWhile(PromiseSupplier<Boolean> predicate, VoidPromiseSupplier block) {
        return just().doDoWhileVoid(predicate, block);
    }

    public static <E> Promise<Void> doForEachObject(Object iterableOrArray, VoidPromiseFunction<E> block) {
        if (iterableOrArray == null) {
            return just();
        } else if (iterableOrArray.getClass().isArray() && !iterableOrArray.getClass().getComponentType().isPrimitive()) {
            //noinspection unchecked
            return just().doForEachObjectArray((E[]) iterableOrArray, block);
        } else if (iterableOrArray instanceof Iterable) {
            //noinspection unchecked
            return just().doForEachIterable((Iterable<E>) iterableOrArray, block);
        } else {
            throw new IllegalArgumentException("The iterable must be object array or iterable, but it is " + iterableOrArray.getClass().getName() + ".");
        }
    }

    public static Promise<Void> doForEachByte(Object iterableOrArray, ByteVoidPromiseFunction block) {
        if (iterableOrArray == null) {
            return just();
        } else if (iterableOrArray instanceof byte[]) {
            return just().doForEachByteArray((byte[]) iterableOrArray, block);
        } else if (iterableOrArray instanceof Iterable) {
            //noinspection unchecked
            return just().doForEachIterable((Iterable<Byte>) iterableOrArray, block::apply);
        } else {
            throw new IllegalArgumentException("The iterable must be byte array or iterable of Byte, but it is " + iterableOrArray.getClass().getName() + ".");
        }
    }

    public static Promise<Void> doForEachChar(Object iterableOrArray, CharVoidPromiseFunction block) {
        if (iterableOrArray == null) {
            return just();
        } else if (iterableOrArray instanceof char[]) {
            return just().doForEachCharArray((char[]) iterableOrArray, block);
        } else if (iterableOrArray instanceof Iterable) {
            //noinspection unchecked
            return just().doForEachIterable((Iterable<Character>) iterableOrArray, block::apply);
        } else {
            throw new IllegalArgumentException("The iterable must be char array or iterable of Character, but it is " + iterableOrArray.getClass().getName() + ".");
        }
    }

    public static Promise<Void> doForEachBoolean(Object iterableOrArray, BooleanVoidPromiseFunction block) {
        if (iterableOrArray == null) {
            return just();
        } else if (iterableOrArray instanceof boolean[]) {
            return just().doForEachBooleanArray((boolean[]) iterableOrArray, block);
        } else if (iterableOrArray instanceof Iterable) {
            //noinspection unchecked
            return just().doForEachIterable((Iterable<Boolean>) iterableOrArray, block::apply);
        } else {
            throw new IllegalArgumentException("The iterable must be boolean array or iterable of Boolean, but it is " + iterableOrArray.getClass().getName() + ".");
        }
    }

    public static Promise<Void> doForEachShort(Object iterableOrArray, ShortVoidPromiseFunction block) {
        if (iterableOrArray == null) {
            return just();
        } else if (iterableOrArray instanceof short[]) {
            return just().doForEachShortArray((short[]) iterableOrArray, block);
        } else if (iterableOrArray instanceof Iterable) {
            //noinspection unchecked
            return just().doForEachIterable((Iterable<Short>) iterableOrArray, block::apply);
        } else {
            throw new IllegalArgumentException("The iterable must be short array or iterable of Short, but it is " + iterableOrArray.getClass().getName() + ".");
        }
    }

    public static Promise<Void> doForEachInt(Object iterableOrArray, IntVoidPromiseFunction block) {
        if (iterableOrArray == null) {
            return just();
        } else if (iterableOrArray instanceof int[]) {
            return just().doForEachIntArray((int[]) iterableOrArray, block);
        } else if (iterableOrArray instanceof Iterable) {
            //noinspection unchecked
            return just().doForEachIterable((Iterable<Integer>) iterableOrArray, block::apply);
        } else {
            throw new IllegalArgumentException("The iterable must be int array or iterable of Integer, but it is " + iterableOrArray.getClass().getName() + ".");
        }
    }

    public static Promise<Void> doForEachLong(Object iterableOrArray, LongVoidPromiseFunction block) {
        if (iterableOrArray == null) {
            return just();
        } else if (iterableOrArray instanceof long[]) {
            return just().doForEachLongArray((long[]) iterableOrArray, block);
        } else if (iterableOrArray instanceof Iterable) {
            //noinspection unchecked
            return just().doForEachIterable((Iterable<Long>) iterableOrArray, block::apply);
        } else {
            throw new IllegalArgumentException("The iterable must be long array or iterable of Long, but it is " + iterableOrArray.getClass().getName() + ".");
        }
    }

    public static Promise<Void> doForEachFloat(Object iterableOrArray, FloatVoidPromiseFunction block) {
        if (iterableOrArray == null) {
            return just();
        } else if (iterableOrArray instanceof float[]) {
            return just().doForEachFloatArray((float[]) iterableOrArray, block);
        } else if (iterableOrArray instanceof Iterable) {
            //noinspection unchecked
            return just().doForEachIterable((Iterable<Float>) iterableOrArray, block::apply);
        } else {
            throw new IllegalArgumentException("The iterable must be float array or iterable of Float, but it is " + iterableOrArray.getClass().getName() + ".");
        }
    }

    public static Promise<Void> doForEachDouble(Object iterableOrArray, DoubleVoidPromiseFunction block) {
        if (iterableOrArray == null) {
            return just();
        } else if (iterableOrArray instanceof double[]) {
            return just().doForEachDoubleArray((double[]) iterableOrArray, block);
        } else if (iterableOrArray instanceof Iterable) {
            //noinspection unchecked
            return just().doForEachIterable((Iterable<Double>) iterableOrArray, block::apply);
        } else {
            throw new IllegalArgumentException("The iterable must be double array or iterable of Double, but it is " + iterableOrArray.getClass().getName() + ".");
        }
    }

    public static boolean mustRethrowException(Throwable t, List<Class<? extends Throwable>> exceptionsType) {
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
