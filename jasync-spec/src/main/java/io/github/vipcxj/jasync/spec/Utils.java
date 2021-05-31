package io.github.vipcxj.jasync.spec;

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
}
