package io.github.vipcxj.jasync.reactive;

import io.github.vipcxj.jasync.ng.runtime.promise.AbstractPromise;
import io.github.vipcxj.jasync.ng.spec.JContext;
import org.reactivestreams.Publisher;

public class ReactivePromise<T> extends AbstractPromise<T> {

    private final JAsyncSubscriber<T> subscriber;
    private final Publisher<T> publisher;
    private final long request;

    public ReactivePromise(Publisher<T> publisher, JAsyncSubscriber<T> subscriber, long request) {
        super(null);
        this.subscriber = subscriber;
        this.publisher = publisher;
        this.request = request;
    }

    @Override
    protected void run(JContext context) {
        subscriber.subscribe(publisher);
        subscriber.addPromiseContext(new PromiseContext<>(this, context, request));
    }

    @Override
    protected void dispose() {
        subscriber.cancel();
    }
}
