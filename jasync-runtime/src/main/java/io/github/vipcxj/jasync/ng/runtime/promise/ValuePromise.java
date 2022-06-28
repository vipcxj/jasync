package io.github.vipcxj.jasync.ng.runtime.promise;

import io.github.vipcxj.jasync.ng.spec.JContext;

public class ValuePromise<T> extends AbstractPromise<T> {

    public ValuePromise(T value) {
        super(null);
        this.value = value;
    }


    @Override
    protected void run(JContext context) {
        resolve(value, context);
    }

    @Override
    protected void dispose() { }
}
