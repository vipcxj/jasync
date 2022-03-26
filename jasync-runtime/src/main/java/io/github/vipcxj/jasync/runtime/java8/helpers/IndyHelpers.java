package io.github.vipcxj.jasync.runtime.java8.helpers;

import io.github.vipcxj.jasync.spec.JPromise;
import io.github.vipcxj.jasync.spec.functional.*;

import java.lang.invoke.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IndyHelpers {

    final Map<String, CallSite> callSites;
    final Class<?> callerClass;
    final MethodHandles.Lookup lookup;

    public IndyHelpers(MethodHandles.Lookup lookup) {
        this.callerClass = lookup.lookupClass();
        this.lookup = lookup;
        this.callSites = new ConcurrentHashMap<>();
    }

    public IndyHelper instance(Object thisObj) {
        return new IndyHelper(this, thisObj);
    }

    private Object[] prependThisArg(Object[] args, Object thisObj) {
        Object[] newArgs = new Object[args.length + 1];
        newArgs[0] = thisObj;
        System.arraycopy(args, 0, newArgs, 1, args.length);
        return newArgs;
    }

    /**
     *
     * @param proxyType the proxy type
     * @param proxyMethodName the method name of the proxy
     * @param proxyMethodType the method type of the proxy.
     * @param implMethodName the impl method name
     * @param implMethodType the impl method type (if instance method, not include this object)
     * @param isStatic whether the impl method is static or instance method
     * @param args the init args (not include this object)
     * @param <T> the created interface type
     * @return the created interface instance
     */
    protected  <T> T createFunction(
            Class<T> proxyType, String proxyMethodName, MethodType proxyMethodType,
            String implMethodName, MethodType implMethodType,
            boolean isStatic, Object thisObj, Object... args
    ) {
        try {
            final CallSite site = callSites.computeIfAbsent(implMethodName, k -> {
                try {
                    // 被代理方法的参数由额外的初始化参数+
                    // 代理类的初始化参数个数，不包括this
                    int initParamsNum = args.length;
                    // 实际被调用方法的参数个数。实际被调用方法的参数因由初始化参数（不包括this）+接口方法参数构成
                    int implParamsNum = implMethodType.parameterCount();
                    // 实际被调用方法
                    MethodHandle implMethod = isStatic
                            ? lookup.findStatic(callerClass, implMethodName, implMethodType)
                            : lookup.findSpecial(callerClass, implMethodName, implMethodType, callerClass);
                    // 生成接口的方法类型，所以返回值类型必定是接口类型。当实际被调用接口为实例方法，初始化额外需要this对象，排在所有其他初始化参数之前
                    Class<?>[] initParamTypes = implMethodType.dropParameterTypes(initParamsNum, implParamsNum).parameterArray();
                    MethodType interfaceInitType = isStatic
                            ? MethodType.methodType(proxyType, initParamTypes)
                            : MethodType.methodType(proxyType, callerClass, initParamTypes);
                    // 接口目标方法的类型。实际被调用方法去掉初始化参数即为所需，但需要类型擦除，所以这里使用反射
                    // 被用户具体执行的方法的类型。可能就是接口目标方法的类型，但也可能更具体，这里取实际被调用方法去掉初始化参数
                    MethodType actualFunMethodType = maybeBoxOrUnBox(implMethodType.dropParameterTypes(0, initParamsNum), proxyMethodType);
                    return LambdaMetafactory.metafactory(lookup,
                            proxyMethodName,
                            interfaceInitType,
                            proxyMethodType,
                            implMethod,
                            actualFunMethodType);
                } catch (LambdaConversionException | NoSuchMethodException | IllegalAccessException e) {
                    throw  new RuntimeException(e);
                }
            });
            //noinspection unchecked
            return site != null ? (T) site.getTarget().invokeWithArguments(isStatic ? args : prependThisArg(args, thisObj)) : null;
        } catch (Throwable e) {
            throw  new RuntimeException(e);
        }
    }

    private Class<?> boxClass(Class<?> type) {
        if (type == int.class) {
            return Integer.class;
        } else if (type == boolean.class) {
            return Boolean.class;
        } else if (type == float.class) {
            return Float.class;
        } else if (type == double.class) {
            return Double.class;
        } else if (type == long.class) {
            return Long.class;
        } else if (type == byte.class) {
            return Byte.class;
        } else if (type == char.class) {
            return Character.class;
        } else if (type == short.class) {
            return Short.class;
        } else {
            throw new IllegalArgumentException("The type " + type + " is not a primitive class.");
        }
    }

    private Class<?> maybeBoxOrUnBox(Class<?> actualType, Class<?> targetType) {
        if (actualType.isPrimitive() && !targetType.isPrimitive()) {
            return boxClass(actualType);
        } else if (targetType.isPrimitive() && !actualType.isPrimitive()) {
            return targetType;
        }
        return actualType;
    }

    private MethodType maybeBoxOrUnBox(MethodType actualMethodType, MethodType targetMethodType) {
        Class<?> actualReturnType = actualMethodType.returnType();
        Class<?> targetReturnType = targetMethodType.returnType();
        Class<?> newActualReturnType = maybeBoxOrUnBox(actualReturnType, targetReturnType);
        if (newActualReturnType != actualReturnType) {
            actualMethodType = actualMethodType.changeReturnType(newActualReturnType);
        }
        int count = actualMethodType.parameterCount();
        for (int i = 0; i < count; ++i) {
            Class<?> actualType = actualMethodType.parameterType(i);
            Class<?> targetType = targetMethodType.parameterType(i);
            Class<?> newActualType = maybeBoxOrUnBox(actualType, targetType);
            if (newActualType != actualType) {
                actualMethodType = actualMethodType.changeParameterType(i, newActualType);
            }
        }
        return actualMethodType;
    }

    public BooleanSupplier booleanSupplier(String method, MethodType methodType, Object... args) {
        return createFunction(
                BooleanSupplier.class, "getAsBoolean", MethodType.methodType(boolean.class),
                method, methodType,
                true, null, args
        );
    }

    public BooleanVoidPromiseFunction booleanVoidPromiseFunction(String method, MethodType methodType, Object... args) {
        return createFunction(
                BooleanVoidPromiseFunction.class,
                "apply",
                MethodType.methodType(JPromise.class, boolean.class),
                method, methodType,
                true, null, args
        );
    }

    public ByteVoidPromiseFunction byteVoidPromiseFunction(String method, MethodType methodType, Object... args) {
        return createFunction(
                ByteVoidPromiseFunction.class,
                "apply",
                MethodType.methodType(JPromise.class, byte.class),
                method, methodType,
                true, null, args
        );
    }

    public CharVoidPromiseFunction charVoidPromiseFunction(String method, MethodType methodType, Object... args) {
        return createFunction(
                CharVoidPromiseFunction.class,
                "apply",
                MethodType.methodType(JPromise.class, char.class),
                method, methodType,
                true, null, args
        );
    }

    public DoubleVoidPromiseFunction doubleVoidPromiseFunction(String method, MethodType methodType, Object... args) {
        return createFunction(
                DoubleVoidPromiseFunction.class,
                "apply",
                MethodType.methodType(JPromise.class, double.class),
                method, methodType,
                true, null, args
        );
    }

    public FloatVoidPromiseFunction floatVoidPromiseFunction(String method, MethodType methodType, Object... args) {
        return createFunction(
                FloatVoidPromiseFunction.class,
                "apply",
                MethodType.methodType(JPromise.class, float.class),
                method, methodType,
                true, null, args
        );
    }

    public IntVoidPromiseFunction intVoidPromiseFunction(String method, MethodType methodType, Object... args) {
        return createFunction(
                IntVoidPromiseFunction.class,
                "apply",
                MethodType.methodType(JPromise.class, int.class),
                method, methodType,
                true, null, args
        );
    }

    public LongVoidPromiseFunction longVoidPromiseFunction(String method, MethodType methodType, Object... args) {
        return createFunction(
                LongVoidPromiseFunction.class,
                "apply",
                MethodType.methodType(JPromise.class, long.class),
                method, methodType,
                true, null, args
        );
    }

    public <T, R> PromiseFunction<T, R> promiseFunction(String method, MethodType methodType, Object... args) {
        //noinspection unchecked
        return createFunction(
                PromiseFunction.class,
                "apply",
                MethodType.methodType(JPromise.class, Object.class),
                method, methodType,
                true, null, args
        );
    }

    public <T> PromiseSupplier<T> promiseSupplier(String method, MethodType methodType, Object... args) {
        //noinspection unchecked
        return createFunction(
                PromiseSupplier.class, "get", MethodType.methodType(JPromise.class),
                method, methodType,
                true, null, args
        );
    }

    public ShortVoidPromiseFunction shortVoidPromiseFunction(String method, MethodType methodType, Object... args) {
        return createFunction(
                ShortVoidPromiseFunction.class,
                "apply",
                MethodType.methodType(JPromise.class, short.class),
                method, methodType,
                true, null, args
        );
    }

    public <T extends Throwable> ThrowableConsumer<T>  throwableConsumer(String method, MethodType methodType, Object... args) {
        //noinspection unchecked
        return createFunction(
                ThrowableConsumer.class,
                "accept",
                MethodType.methodType(void.class, Throwable.class),
                method, methodType,
                true, null, args
        );
    }

    public  <T> VoidPromiseFunction<T> voidPromiseFunction(String method, MethodType methodType, Object... args) {
        //noinspection unchecked
        return createFunction(
                VoidPromiseFunction.class, "apply", MethodType.methodType(JPromise.class, Object.class),
                method, methodType,
                true, null, args
        );
    }

    public VoidPromiseSupplier voidPromiseSupplier(String method, MethodType methodType, Object... args) {
        return createFunction(
                VoidPromiseSupplier.class, "get", MethodType.methodType(JPromise.class),
                method, methodType,
                true, null, args
        );
    }
}
