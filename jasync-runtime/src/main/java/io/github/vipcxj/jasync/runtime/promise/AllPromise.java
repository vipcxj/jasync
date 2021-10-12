package io.github.vipcxj.jasync.runtime.promise;

import io.github.vipcxj.jasync.runtime.schedule.AllPromisesTask;
import io.github.vipcxj.jasync.spec.JPromise2;

import java.util.List;

public class AllPromise<T> extends BasePromise<List<T>> {
    public AllPromise(List<JPromise2<? extends T>> promises) {
        super(new AllPromisesTask<>(promises));
    }
}
