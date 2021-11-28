package io.github.vipcxj.jasync.spec;

public interface JPushContext {
    JPushContext push(Object v);
    JPromise2<JContext> complete();
}
