package io.github.vipcxj.jasync.runtime.promise;

import io.github.vipcxj.jasync.runtime.schedule.ErrorTask;

public class ErrorPromise<T> extends BasePromise<T> {

    public ErrorPromise(Throwable error) {
        super(new ErrorTask<>(error));
    }
}
