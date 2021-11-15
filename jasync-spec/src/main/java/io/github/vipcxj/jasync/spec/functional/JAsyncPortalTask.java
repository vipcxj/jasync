package io.github.vipcxj.jasync.spec.functional;

import io.github.vipcxj.jasync.spec.JContext;
import io.github.vipcxj.jasync.spec.JPromise2;
import io.github.vipcxj.jasync.spec.JPortal;

public interface JAsyncPortalTask<T> {
    JPromise2<T> invoke(JPortal<T> factory, JContext context);
}
