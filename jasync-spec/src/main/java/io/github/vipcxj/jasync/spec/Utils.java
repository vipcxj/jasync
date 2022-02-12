package io.github.vipcxj.jasync.spec;

import io.github.vipcxj.jasync.spec.spi.PrioritySupport;

import java.util.ServiceLoader;

public class Utils {

    public static <T extends PrioritySupport> T getProvider(Class<T> type) {
        ServiceLoader<T> loader = ServiceLoader.load(type);
        int p = PrioritySupport.PRIORITY_MIN;
        T result = null;
        for (T next : loader) {
            if (next.priority() > p) {
                result = next;
                p = next.priority();
            }
        }
        if (result == null) {
            throw new IllegalStateException("Unable to find a provider of type " + type.getCanonicalName() + ".");
        }
        return result;
    }
}
