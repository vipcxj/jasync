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
     * 1: requesting
     * 2: requested
     * 3: cancelled
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
                this.state = 2;
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
        if (this.state == 3) {
            return;
        }
        while(true) {
            if (this.state == 3) { // have cancelled.
                return;
            } else if (this.state == 1) { // is cancelling
                if (this.handle != null) {
                    if (STATE_UPDATER.compareAndSet(this, 1, 3)) {
                        this.handle.cancel();
                    }
                }
            } else if (this.state == 2) { // the promise has completed, so no need cancel it, even if the handle has not assigned.
                this.state = 3;
            } else { // the promise has not started.
                STATE_UPDATER.compareAndSet(this, 0, 3);
            }
        }
    }
    
}
