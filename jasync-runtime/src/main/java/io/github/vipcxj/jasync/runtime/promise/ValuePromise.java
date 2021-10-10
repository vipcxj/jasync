package io.github.vipcxj.jasync.runtime.promise;

import io.github.vipcxj.jasync.runtime.schedule.ValueTask;

public class ValuePromise<T> extends BasePromise<T> {

    public ValuePromise(T value) {
        super(new ValueTask<>(value));
    }
}
