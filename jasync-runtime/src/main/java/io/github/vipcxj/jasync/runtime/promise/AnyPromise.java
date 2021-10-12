package io.github.vipcxj.jasync.runtime.promise;

import io.github.vipcxj.jasync.runtime.schedule.AnyPromiseTask;
import io.github.vipcxj.jasync.spec.JPromise2;

import java.util.List;

public class AnyPromise<T> extends BasePromise<T> {
    public AnyPromise(List<JPromise2<? extends T>> promises) {
        super(new AnyPromiseTask<>(promises));
    }
}
