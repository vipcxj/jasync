package io.github.vipcxj.jasync.runtime.java8.helpers;

import io.github.vipcxj.jasync.spec.Promise;
import io.github.vipcxj.jasync.spec.functional.*;

import java.lang.invoke.MethodType;

public class IndyHelper {

    private final IndyHelpers helpers;
    private final Object thisObj;

    public IndyHelper(IndyHelpers helpers, Object thisObj) {
        this.helpers = helpers;
        this.thisObj = thisObj;
    }

    public BooleanSupplier booleanSupplier(String method, MethodType methodType, Object... args) {
        return helpers.createFunction(
                BooleanSupplier.class, "getAsBoolean", MethodType.methodType(boolean.class),
                method, methodType,
                false, thisObj, args
        );
    }

    public BooleanVoidPromiseFunction booleanVoidPromiseFunction(String method, MethodType methodType, Object... args) {
        return helpers.createFunction(
                BooleanVoidPromiseFunction.class,
                "apply",
                MethodType.methodType(Promise.class, boolean.class),
                method, methodType,
                false, thisObj, args
        );
    }

    public ByteVoidPromiseFunction byteVoidPromiseFunction(String method, MethodType methodType, Object... args) {
        return helpers.createFunction(
                ByteVoidPromiseFunction.class,
                "apply",
                MethodType.methodType(Promise.class, byte.class),
                method, methodType,
                false, thisObj, args
        );
    }

    public CharVoidPromiseFunction charVoidPromiseFunction(String method, MethodType methodType, Object... args) {
        return helpers.createFunction(
                CharVoidPromiseFunction.class,
                "apply",
                MethodType.methodType(Promise.class, char.class),
                method, methodType,
                false, thisObj, args
        );
    }

    public DoubleVoidPromiseFunction doubleVoidPromiseFunction(String method, MethodType methodType, Object... args) {
        return helpers.createFunction(
                DoubleVoidPromiseFunction.class,
                "apply",
                MethodType.methodType(Promise.class, double.class),
                method, methodType,
                false, thisObj, args
        );
    }

    public FloatVoidPromiseFunction floatVoidPromiseFunction(String method, MethodType methodType, Object... args) {
        return helpers.createFunction(
                FloatVoidPromiseFunction.class,
                "apply",
                MethodType.methodType(Promise.class, float.class),
                method, methodType,
                false, thisObj, args
        );
    }

    public IntVoidPromiseFunction intVoidPromiseFunction(String method, MethodType methodType, Object... args) {
        return helpers.createFunction(
                IntVoidPromiseFunction.class,
                "apply",
                MethodType.methodType(Promise.class, int.class),
                method, methodType,
                false, thisObj, args
        );
    }

    public LongVoidPromiseFunction longVoidPromiseFunction(String method, MethodType methodType, Object... args) {
        return helpers.createFunction(
                LongVoidPromiseFunction.class,
                "apply",
                MethodType.methodType(Promise.class, long.class),
                method, methodType,
                false, thisObj, args
        );
    }

    public <T, R> PromiseFunction<T, R> promiseFunction(String method, MethodType methodType, Object... args) {
        //noinspection unchecked
        return helpers.createFunction(
                PromiseFunction.class,
                "apply",
                MethodType.methodType(Promise.class, Object.class),
                method, methodType,
                false, thisObj, args
        );
    }

    public <T> PromiseSupplier<T> promiseSupplier(String method, MethodType methodType, Object... args) {
        //noinspection unchecked
        return helpers.createFunction(
                PromiseSupplier.class, "get", MethodType.methodType(Promise.class),
                method, methodType,
                false, thisObj, args
        );
    }

    public ShortVoidPromiseFunction shortVoidPromiseFunction(String method, MethodType methodType, Object... args) {
        return helpers.createFunction(
                ShortVoidPromiseFunction.class,
                "apply",
                MethodType.methodType(Promise.class, short.class),
                method, methodType,
                false, thisObj, args
        );
    }

    public <T extends Throwable> ThrowableConsumer<T>  throwableConsumer(String method, MethodType methodType, Object... args) {
        //noinspection unchecked
        return helpers.createFunction(
                ThrowableConsumer.class,
                "accept",
                MethodType.methodType(void.class, Throwable.class),
                method, methodType,
                false, thisObj, args
        );
    }

    public  <T> VoidPromiseFunction<T> voidPromiseFunction(String method, MethodType methodType, Object... args) {
        //noinspection unchecked
        return helpers.createFunction(
                VoidPromiseFunction.class, "apply", MethodType.methodType(Promise.class, Object.class),
                method, methodType,
                false, thisObj, args
        );
    }

    public VoidPromiseSupplier voidPromiseSupplier(String method, MethodType methodType, Object... args) {
        return helpers.createFunction(
                VoidPromiseSupplier.class, "get", MethodType.methodType(Promise.class),
                method, methodType,
                false, thisObj, args
        );
    }

}
