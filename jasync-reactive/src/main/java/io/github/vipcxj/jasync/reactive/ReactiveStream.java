package io.github.vipcxj.jasync.reactive;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import org.reactivestreams.Publisher;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class ReactiveStream<T> {

    private final Publisher<T> publisher;
    private volatile JAsyncSubscriber<T> subscriber;
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<ReactiveStream, JAsyncSubscriber> SUBSCRIBER = AtomicReferenceFieldUpdater.newUpdater(ReactiveStream.class, JAsyncSubscriber.class, "subscriber");
    private volatile int state;


    public ReactiveStream(Publisher<T> publisher) {
        this.publisher = publisher;
    }

    private JAsyncSubscriber<T> getSubscriber(long n) {
        while (true) {
            if (subscriber == null && SUBSCRIBER.compareAndSet(this, null, new JAsyncSubscriber<>(n))) {
                return subscriber;
            } else if (subscriber != null) {
                return subscriber;
            }
        }
    }

    public JPromise<T> request(long n) {
        return new ReactivePromise<>(publisher, getSubscriber(n), n);
    }
}
