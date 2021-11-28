package io.github.vipcxj.jasync.spec.functional;

import io.github.vipcxj.jasync.spec.JPortal;
import io.github.vipcxj.jasync.spec.JPromise2;

public interface JAsyncPortalTask0<T> {
    JPromise2<T> invoke(JPortal<T> factory) throws Throwable;
}
