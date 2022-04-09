package io.github.vipcxj.jasync.ng.runtime.promise;

import io.github.vipcxj.jasync.ng.runtime.schedule.AllPromisesTask;
import io.github.vipcxj.jasync.ng.spec.JPromise;

import java.util.List;

public class AllPromise<T> extends BasePromise<List<T>> {
    public AllPromise(List<JPromise<? extends T>> promises) {
        super(new AllPromisesTask<>(promises));
    }
}
