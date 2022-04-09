package io.github.vipcxj.jasync.ng.core;

public class GlobalThreadLocal<T> extends InheritableThreadLocal<T> {

    public final String key;

    public GlobalThreadLocal(String key) {
        this.key = key;
    }
}
