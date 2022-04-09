package io.github.vipcxj.jasync.ng.spec.functional;

import io.github.vipcxj.jasync.ng.spec.JPortal;
import io.github.vipcxj.jasync.ng.spec.JPromise;

public interface JAsyncPortalTask0<T> {
    JPromise<T> invoke(JPortal<T> factory) throws Throwable;
}
