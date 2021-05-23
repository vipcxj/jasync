package io.github.vipcxj.jasync;

import io.github.vipcxj.jasync.spi.PromiseProvider;

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
