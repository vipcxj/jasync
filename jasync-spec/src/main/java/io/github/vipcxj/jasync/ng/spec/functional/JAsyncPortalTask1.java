package io.github.vipcxj.jasync.ng.spec.functional;

import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JPortal;

public interface JAsyncPortalTask1<T> {
    JPromise<T> invoke(JPortal<T> factory, JContext context) throws Throwable;
}
