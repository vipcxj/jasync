package io.github.vipcxj.jasync.spec;

import io.github.vipcxj.jasync.spec.functional.PromiseFunction;
import io.github.vipcxj.jasync.spec.functional.VoidPromiseSupplier;
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

    public static <T> Promise<T> safeApply(PromiseFunction<T, T> fuc, T v) throws Throwable {
        Promise<T> res = fuc.apply(v);
        return res != null ? res : JAsync.just();
    }

    public static Promise<Void> safeGet(VoidPromiseSupplier fuc) throws Throwable {
        Promise<Void> res = fuc.get();
        return res != null ? res : JAsync.just();
    }
}
