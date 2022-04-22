package io.github.vipcxj.jasync.ng.runtime.promise;

import io.github.vipcxj.jasync.ng.runtime.schedule.RacePromiseTask;
import io.github.vipcxj.jasync.ng.spec.JPromise;

import java.util.List;

public class RacePromise<T> extends BasePromise<T> {

    public RacePromise(List<JPromise<? extends T>> promises) {
        super(new RacePromiseTask<>(promises));
    }
}
