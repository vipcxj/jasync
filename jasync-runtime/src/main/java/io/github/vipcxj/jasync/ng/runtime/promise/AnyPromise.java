package io.github.vipcxj.jasync.ng.runtime.promise;

import io.github.vipcxj.jasync.ng.runtime.schedule.AnyPromiseTask;
import io.github.vipcxj.jasync.ng.spec.JPromise;

import java.util.List;

public class AnyPromise<T> extends BasePromise<T> {
    public AnyPromise(List<JPromise<? extends T>> promises) {
        super(new AnyPromiseTask<>(promises));
    }
}
