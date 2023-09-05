package io.github.vipcxj.jasync.ng.reactive;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JPromiseTrigger;

public class JAsyncSubscriber<T> implements Subscriber<T> {

    private final JPromiseTrigger<Subscription> subscriptionResolver;
    private final JPromiseTrigger<Result<T>> valueResolver;
    private boolean done;
    private Subscription s;

    public JAsyncSubscriber() {
        this.subscriptionResolver = JPromise.createTrigger();
        this.valueResolver = JPromise.createTrigger();
        this.done = false;
    }

    @Override
    public void onSubscribe(Subscription s) {
        this.s = s;
        this.subscriptionResolver.resolve(s);
    }

    @Override
    public void onNext(T t) {
        if (!done) {
            this.done = true;
            this.s.cancel();
            this.valueResolver.resolve(new Result<T>(t, null));
        }
    }

    @Override
    public void onError(Throwable t) {
        if (!done) {
            this.done = true;
            this.valueResolver.resolve(new Result<T>(null, t));
        }
    }

    @Override
    public void onComplete() {
        if (!done) {
            this.done = true;
            this.valueResolver.resolve(new Result<T>(null, null));
        }
    }

    public JPromise<Result<T>> createValuePromise() {
        return valueResolver.getPromise();
    }

    public JPromise<Subscription> createSubscriptionPromise() {
        return subscriptionResolver.getPromise();
    }
    
    public static class Result<T> {
        private final T value;
        private final Throwable throwable;

        public Result(T value, Throwable throwable) {
            this.value = value;
            this.throwable = throwable;
        }

        public T getValue() {
            return value;
        }

        public Throwable getThrowable() {
            return throwable;
        }
    }
}
