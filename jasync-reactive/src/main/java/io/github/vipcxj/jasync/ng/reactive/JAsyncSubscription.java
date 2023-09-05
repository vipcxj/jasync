package io.github.vipcxj.jasync.ng.reactive;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.github.vipcxj.jasync.ng.spec.JHandle;
import io.github.vipcxj.jasync.ng.spec.JPromise;

public class JAsyncSubscription<T> implements Subscription {

    private final JPromise<T> promise;
    private final Subscriber<? super T> subscriber;
    private volatile JHandle<T> handle;
    /**
     * 0: init
     * 1: requested
     * 2: cancelled
     */
    private volatile int state;
    @SuppressWarnings("rawtypes")
    private final static AtomicIntegerFieldUpdater<JAsyncSubscription> STATE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(JAsyncSubscription.class, "state");

    public JAsyncSubscription(JPromise<T> promise, Subscriber<? super T> subscriber) {
        this.promise = promise;
        this.subscriber = subscriber;
        this.state = 0;
    }

    @Override
    public void request(long n) {
        if (this.state != 0) {
            return;
        }
        if (STATE_UPDATER.compareAndSet(this, 0, 1)) {
            this.handle = this.promise.onFinally((v, e) -> {
                if (e != null) {
                    this.subscriber.onError(e);
                } else {
                    this.subscriber.onNext(v);
                    this.subscriber.onComplete();
                }
            }).async();
        }
    }

    @Override
    public void cancel() {
        if (this.state == 2) {
            return;
        }
        while(true) {
            if (this.state == 2) {
                return;
            } else if (this.handle != null && STATE_UPDATER.compareAndSet(this, 1, 2)) {
                this.handle.cancel();
            } else {
                STATE_UPDATER.compareAndSet(this, 0, 2);
            }
        }
    }
    
}
