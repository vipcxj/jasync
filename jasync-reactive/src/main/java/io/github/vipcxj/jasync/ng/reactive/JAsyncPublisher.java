package io.github.vipcxj.jasync.ng.reactive;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.github.vipcxj.jasync.ng.spec.JPromise;

public class JAsyncPublisher<T> implements Publisher<T> {

    private final JPromise<T> promise;

    public JAsyncPublisher(JPromise<T> promise) {
        this.promise = promise;
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        JAsyncSubscription<T> subscription = new JAsyncSubscription<>(this.promise, s);
        s.onSubscribe(subscription);
    }
}
