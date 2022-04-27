package io.github.vipcxj.jasync.ng.runtime.promise;

import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JPromiseTrigger;
import io.github.vipcxj.jasync.ng.spec.JThunk;

import java.util.function.BiConsumer;

public class PromiseTrigger<T> implements JPromiseTrigger<T>, BiConsumer<JThunk<T>, JContext> {

    private final JPromise<T> promise;
    private JThunk<T> thunk;
    private JContext context;

    public PromiseTrigger() {
        this.promise = JPromise.generate(this);
    }

    @Override
    public JPromiseTrigger<T> start(JContext context) {
        promise.async(context);
        return this;
    }

    @Override
    public void resolve(T result) {
        thunk.resolve(result, context);
    }

    @Override
    public void reject(Throwable error) {
        thunk.reject(error, context);
    }

    @Override
    public void cancel() {
        thunk.cancel();
    }

    @Override
    public JPromise<T> getPromise() {
        return promise;
    }

    @Override
    public void accept(JThunk<T> thunk, JContext context) {
        this.thunk = thunk;
        this.context = context;
    }
}
