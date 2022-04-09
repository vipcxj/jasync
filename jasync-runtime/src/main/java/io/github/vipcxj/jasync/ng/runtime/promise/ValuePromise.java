package io.github.vipcxj.jasync.ng.runtime.promise;

import io.github.vipcxj.jasync.ng.runtime.schedule.ValueTask;

public class ValuePromise<T> extends BasePromise<T> {

    public ValuePromise(T value) {
        super(new ValueTask<>(value));
    }
}
