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

    public static Promise<Void> deferVoid(VoidPromiseSupplier block, String label) {
        return defer(block).doCatch(BreakException.class, e -> e.matchLabel(label) ? null : JAsync.error(e));
    }

    public static <T> Promise<T> error(Throwable t) {
        return assertProvider().error(t);
    }

    public static Promise<Void> doIf(boolean test, VoidPromiseSupplier thenDo, VoidPromiseSupplier elseDo) {
        try {
            if (test) {
                return Utils.safeGetVoid(thenDo);
            } else {
                return Utils.safeGetVoid(elseDo);
            }
        } catch (Throwable e) {
            return JAsync.error(e);
        }
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

    public static <C> Promise<Void> doSwitch(C value, List<? extends ICase<C>> cases, String label) {
        return just().doSwitch(value, cases, label);
    }

    public static <C> Promise<Void> doSwitch(C value, List<? extends ICase<C>> cases) {
        return just().doSwitch(value, cases);
    }

    public static Promise<Void> doWhile(BooleanSupplier predicate, VoidPromiseSupplier block, String label) {
        return just().doWhileVoid(predicate, block, label);
    }

    public static Promise<Void> doWhile(BooleanSupplier predicate, VoidPromiseSupplier block) {
        return just().doWhileVoid(predicate, block);
    }

    public static Promise<Void> doWhile(PromiseSupplier<Boolean> predicate, VoidPromiseSupplier block, String label) {
        return just().doWhileVoid(predicate, block, label);
    }

    public static Promise<Void> doWhile(PromiseSupplier<Boolean> predicate, VoidPromiseSupplier block) {
        return just().doWhileVoid(predicate, block);
    }

    public static Promise<Void> doDoWhile(BooleanSupplier predicate, VoidPromiseSupplier block, String label) {
        return just().doDoWhileVoid(predicate, block, label);
    }

    public static Promise<Void> doDoWhile(BooleanSupplier predicate, VoidPromiseSupplier block) {
        return just().doDoWhileVoid(predicate, block);
    }

    public static Promise<Void> doDoWhile(PromiseSupplier<Boolean> predicate, VoidPromiseSupplier block, String label) {
        return just().doDoWhileVoid(predicate, block, label);
    }

    public static Promise<Void> doDoWhile(PromiseSupplier<Boolean> predicate, VoidPromiseSupplier block) {
        return just().doDoWhileVoid(predicate, block);
    }

    public static <E> Promise<Void> doForEachObject(Object iterableOrArray, VoidPromiseFunction<E> block, String label) {
        if (iterableOrArray == null) {
            return just();
        } else if (iterableOrArray.getClass().isArray() && !iterableOrArray.getClass().getComponentType().isPrimitive()) {
            //noinspection unchecked
            return just().doForEachObjectArray((E[]) iterableOrArray, block, label);
        } else if (iterableOrArray instanceof Iterable) {
            //noinspection unchecked
            return just().doForEachIterable((Iterable<E>) iterableOrArray, block, label);
        } else {
            throw new IllegalArgumentException("The iterable must be object array or iterable, but it is " + iterableOrArray.getClass().getName() + ".");
        }
    }

    public static <E> Promise<Void> doForEachObject(Object iterableOrArray, VoidPromiseFunction<E> block) {
        return doForEachObject(iterableOrArray, block, null);
    }

    public static Promise<Void> doForEachByte(Object iterableOrArray, ByteVoidPromiseFunction block, String label) {
        if (iterableOrArray == null) {
            return just();
        } else if (iterableOrArray instanceof byte[]) {
            return just().doForEachByteArray((byte[]) iterableOrArray, block, label);
        } else if (iterableOrArray instanceof Iterable) {
            //noinspection unchecked
            return just().doForEachIterable((Iterable<Byte>) iterableOrArray, block::apply, label);
        } else {
            throw new IllegalArgumentException("The iterable must be byte array or iterable of Byte, but it is " + iterableOrArray.getClass().getName() + ".");
        }
    }

    public static Promise<Void> doForEachByte(Object iterableOrArray, ByteVoidPromiseFunction block) {
        return doForEachByte(iterableOrArray, block, null);
    }

    public static Promise<Void> doForEachChar(Object iterableOrArray, CharVoidPromiseFunction block, String label) {
        if (iterableOrArray == null) {
            return just();
        } else if (iterableOrArray instanceof char[]) {
            return just().doForEachCharArray((char[]) iterableOrArray, block, label);
        } else if (iterableOrArray instanceof Iterable) {
            //noinspection unchecked
            return just().doForEachIterable((Iterable<Character>) iterableOrArray, block::apply, label);
        } else {
            throw new IllegalArgumentException("The iterable must be char array or iterable of Character, but it is " + iterableOrArray.getClass().getName() + ".");
        }
    }

    public static Promise<Void> doForEachChar(Object iterableOrArray, CharVoidPromiseFunction block) {
        return doForEachChar(iterableOrArray, block, null);
    }

    public static Promise<Void> doForEachBoolean(Object iterableOrArray, BooleanVoidPromiseFunction block, String label) {
        if (iterableOrArray == null) {
            return just();
        } else if (iterableOrArray instanceof boolean[]) {
            return just().doForEachBooleanArray((boolean[]) iterableOrArray, block, label);
        } else if (iterableOrArray instanceof Iterable) {
            //noinspection unchecked
            return just().doForEachIterable((Iterable<Boolean>) iterableOrArray, block::apply, label);
        } else {
            throw new IllegalArgumentException("The iterable must be boolean array or iterable of Boolean, but it is " + iterableOrArray.getClass().getName() + ".");
        }
    }

    public static Promise<Void> doForEachBoolean(Object iterableOrArray, BooleanVoidPromiseFunction block) {
        return doForEachBoolean(iterableOrArray, block, null);
    }

    public static Promise<Void> doForEachShort(Object iterableOrArray, ShortVoidPromiseFunction block, String label) {
        if (iterableOrArray == null) {
            return just();
        } else if (iterableOrArray instanceof short[]) {
            return just().doForEachShortArray((short[]) iterableOrArray, block, label);
        } else if (iterableOrArray instanceof Iterable) {
            //noinspection unchecked
            return just().doForEachIterable((Iterable<Short>) iterableOrArray, block::apply, label);
        } else {
            throw new IllegalArgumentException("The iterable must be short array or iterable of Short, but it is " + iterableOrArray.getClass().getName() + ".");
        }
    }

    public static Promise<Void> doForEachShort(Object iterableOrArray, ShortVoidPromiseFunction block) {
        return doForEachShort(iterableOrArray, block, null);
    }

    public static Promise<Void> doForEachInt(Object iterableOrArray, IntVoidPromiseFunction block, String label) {
        if (iterableOrArray == null) {
            return just();
        } else if (iterableOrArray instanceof int[]) {
            return just().doForEachIntArray((int[]) iterableOrArray, block, label);
        } else if (iterableOrArray instanceof Iterable) {
            //noinspection unchecked
            return just().doForEachIterable((Iterable<Integer>) iterableOrArray, block::apply, label);
        } else {
            throw new IllegalArgumentException("The iterable must be int array or iterable of Integer, but it is " + iterableOrArray.getClass().getName() + ".");
        }
    }

    public static Promise<Void> doForEachInt(Object iterableOrArray, IntVoidPromiseFunction block) {
        return doForEachInt(iterableOrArray, block, null);
    }

    public static Promise<Void> doForEachLong(Object iterableOrArray, LongVoidPromiseFunction block, String label) {
        if (iterableOrArray == null) {
            return just();
        } else if (iterableOrArray instanceof long[]) {
            return just().doForEachLongArray((long[]) iterableOrArray, block, label);
        } else if (iterableOrArray instanceof Iterable) {
            //noinspection unchecked
            return just().doForEachIterable((Iterable<Long>) iterableOrArray, block::apply, label);
        } else {
            throw new IllegalArgumentException("The iterable must be long array or iterable of Long, but it is " + iterableOrArray.getClass().getName() + ".");
        }
    }

    public static Promise<Void> doForEachLong(Object iterableOrArray, LongVoidPromiseFunction block) {
        return doForEachLong(iterableOrArray, block, null);
    }

    public static Promise<Void> doForEachFloat(Object iterableOrArray, FloatVoidPromiseFunction block, String label) {
        if (iterableOrArray == null) {
            return just();
        } else if (iterableOrArray instanceof float[]) {
            return just().doForEachFloatArray((float[]) iterableOrArray, block, label);
        } else if (iterableOrArray instanceof Iterable) {
            //noinspection unchecked
            return just().doForEachIterable((Iterable<Float>) iterableOrArray, block::apply, label);
        } else {
            throw new IllegalArgumentException("The iterable must be float array or iterable of Float, but it is " + iterableOrArray.getClass().getName() + ".");
        }
    }

    public static Promise<Void> doForEachFloat(Object iterableOrArray, FloatVoidPromiseFunction block) {
        return doForEachFloat(iterableOrArray, block, null);
    }

    public static Promise<Void> doForEachDouble(Object iterableOrArray, DoubleVoidPromiseFunction block, String label) {
        if (iterableOrArray == null) {
            return just();
        } else if (iterableOrArray instanceof double[]) {
            return just().doForEachDoubleArray((double[]) iterableOrArray, block, label);
        } else if (iterableOrArray instanceof Iterable) {
            //noinspection unchecked
            return just().doForEachIterable((Iterable<Double>) iterableOrArray, block::apply, label);
        } else {
            throw new IllegalArgumentException("The iterable must be double array or iterable of Double, but it is " + iterableOrArray.getClass().getName() + ".");
        }
    }

    public static Promise<Void> doForEachDouble(Object iterableOrArray, DoubleVoidPromiseFunction block) {
        return doForEachDouble(iterableOrArray, block, null);
    }

    private static Promise<Void> doForBody(VoidPromiseSupplier step, VoidPromiseSupplier body) throws Throwable {
        return Utils.safeGetVoid(body).doFinally(() -> {
            Utils.safeGetVoid(step);
            return null;
        });
    }

    public static Promise<Void> doFor(VoidPromiseSupplier init, BooleanSupplier cond, VoidPromiseSupplier step, VoidPromiseSupplier body, String label) {
        if (cond == null) {
            cond = () -> true;
        }
        try {
            return Utils.safeGetVoid(init).doWhileVoid(cond, () -> doForBody(step, body), label);
        } catch (Throwable t) {
            return JAsync.error(t);
        }
    }

    public static Promise<Void> doFor(VoidPromiseSupplier init, PromiseSupplier<Boolean> cond, VoidPromiseSupplier step, VoidPromiseSupplier body, String label) {
        if (cond == null) {
            cond = () -> JAsync.just(true);
        }
        try {
            return Utils.safeGetVoid(init).doWhileVoid(cond, () -> doForBody(step, body), label);
        } catch (Throwable t) {
            return JAsync.error(t);
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
