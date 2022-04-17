package io.github.vipcxj.jasync.ng.spec;

import io.github.vipcxj.jasync.ng.spec.annotations.Internal;

@Internal
public interface JPushContext {
    JPushContext push(Object v);
    JPromise<JContext> complete();
}
