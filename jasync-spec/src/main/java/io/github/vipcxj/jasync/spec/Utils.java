package io.github.vipcxj.jasync.spec;

import io.github.vipcxj.jasync.spec.functional.*;
import io.github.vipcxj.jasync.spec.spi.PromiseProvider;

import java.util.Iterator;
import java.util.ServiceLoader;

public class Utils {

    public static PromiseProvider getProvider() {
        ServiceLoader<PromiseProvider> loader = ServiceLoader.load(PromiseProvider.class);
        Iterator<PromiseProvider> iterator = loader.iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        } else {
            return null;
        }
    }

    public static <T, O> Promise<O> safeApply(PromiseFunction<T, O> fuc, T v) throws Throwable {
        Promise<O> res = fuc != null ? fuc.apply(v) : null;
        return res != null ? res : JAsync.just();
    }

    public static Promise<Void> safeApply(BooleanVoidPromiseFunction fuc, boolean v) throws Throwable {
        Promise<Void> res = fuc != null ? fuc.apply(v) : null;
        return res != null ? res : JAsync.just();
    }

    public static Promise<Void> safeApply(ByteVoidPromiseFunction fuc, byte v) throws Throwable {
        Promise<Void> res = fuc != null ? fuc.apply(v) : null;
        return res != null ? res : JAsync.just();
    }

    public static Promise<Void> safeApply(CharVoidPromiseFunction fuc, char v) throws Throwable {
        Promise<Void> res = fuc != null ? fuc.apply(v) : null;
        return res != null ? res : JAsync.just();
    }

    public static Promise<Void> safeApply(ShortVoidPromiseFunction fuc, short v) throws Throwable {
        Promise<Void> res = fuc != null ? fuc.apply(v) : null;
        return res != null ? res : JAsync.just();
    }

    public static Promise<Void> safeApply(IntVoidPromiseFunction fuc, int v) throws Throwable {
        Promise<Void> res = fuc != null ? fuc.apply(v) : null;
        return res != null ? res : JAsync.just();
    }

    public static Promise<Void> safeApply(LongVoidPromiseFunction fuc, long v) throws Throwable {
        Promise<Void> res = fuc != null ? fuc.apply(v) : null;
        return res != null ? res : JAsync.just();
    }

    public static Promise<Void> safeApply(FloatVoidPromiseFunction fuc, float v) throws Throwable {
        Promise<Void> res = fuc != null ? fuc.apply(v) : null;
        return res != null ? res : JAsync.just();
    }

    public static Promise<Void> safeApply(DoubleVoidPromiseFunction fuc, double v) throws Throwable {
        Promise<Void> res = fuc != null ? fuc.apply(v) : null;
        return res != null ? res : JAsync.just();
    }

    public static Promise<Void> safeGetVoid(VoidPromiseSupplier fuc) throws Throwable {
        Promise<Void> res = fuc != null ? fuc.get() : null;
        return res != null ? res : JAsync.just();
    }

    public static <T> Promise<T> safeGet(PromiseSupplier<T> fuc) throws Throwable {
        Promise<T> res = fuc != null ? fuc.get() : null;
        return res != null ? res : JAsync.just();
    }

    public static boolean safeTest(BooleanSupplier fun) throws Throwable {
        return fun != null && fun.getAsBoolean();
    }

    public static Promise<Boolean> safeTest(PromiseSupplier<Boolean> fun) throws Throwable {
        Promise<Boolean> res = fun != null ? fun.get() : null;
        return res != null ? res.then(test -> JAsync.just(Boolean.TRUE.equals(test))) : JAsync.just(false);
    }
}
