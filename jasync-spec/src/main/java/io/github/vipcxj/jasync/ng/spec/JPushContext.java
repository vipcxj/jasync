package io.github.vipcxj.jasync.ng.spec;

public interface JPushContext {
    JPushContext push(Object v);
    JPromise<JContext> complete();
}
