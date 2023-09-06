package io.github.vipcxj.jasync.ng.reactive;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JPromiseTrigger;
import io.github.vipcxj.jasync.ng.spec.JThunk;

public class JAsyncSubscriber<T> implements Subscriber<T> {

    private final JPromiseTrigger<T> valueResolver;
    private final JPromise<T> promise;
    private boolean done;
    private Subscription s;

    public JAsyncSubscriber() {
        this.valueResolver = JPromise.createTrigger();
        this.done = false;
        this.promise = JPromise.generate(this::onPromiseStart);
    }

    @Override
    public void onSubscribe(Subscription s) {
        this.s = s;
        s.request(Long.MAX_VALUE);
    }

    private void onPromiseStart(JThunk<T> thunk, JContext context) {
        this.valueResolver.getPromise().onFinally((v, t) -> {
            if (t != null) {
                thunk.reject(t, context);
            } else {
                thunk.resolve(v, context);
            }
        }).async();
    }

    @Override
    public void onNext(T t) {
        if (!done) {
            this.done = true;
            this.s.cancel();
            this.valueResolver.resolve(t);
        }
    }

    @Override
    public void onError(Throwable t) {
        if (!done) {
            this.done = true;
            this.valueResolver.reject(t);
        }
    }

    @Override
    public void onComplete() {
        if (!done) {
            this.done = true;
            this.valueResolver.resolve(null);
        }
    }

    public JPromise<T> getPromise() {
        return promise;
    }
}
