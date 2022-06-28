package io.github.vipcxj.jasync.ng.runtime.promise;

import io.github.vipcxj.jasync.ng.spec.JContext;

public class ErrorPromise<T> extends AbstractPromise<T> {

    public ErrorPromise(Throwable error) {
        super(null);
        this.error = error;
    }

    @Override
    protected void run(JContext context) {
        reject(error, context);
    }

    @Override
    protected void dispose() { }
}
