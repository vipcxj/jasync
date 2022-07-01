package io.github.vipcxj.jasync.reactive;

import io.github.vipcxj.jasync.ng.runtime.utils.UnPaddedLockFreeArrayQueue0;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class JAsyncSubscriber<T> implements Subscriber<T> {

    private final long initRequest;
    private final Queue<T> cache;
    private final Queue<PromiseContext<T>> contexts;
    private Throwable error;
    private volatile long remain;
    @SuppressWarnings("rawtypes")
    private static final AtomicLongFieldUpdater<JAsyncSubscriber> REMAIN = AtomicLongFieldUpdater.newUpdater(JAsyncSubscriber.class, "remain");
    private volatile int state;
    @SuppressWarnings("rawtypes")
    private static final AtomicIntegerFieldUpdater<JAsyncSubscriber> STATE = AtomicIntegerFieldUpdater.newUpdater(JAsyncSubscriber.class, "state");
    private static final int ST_READY = 0;
    private static final int ST_CANCEL = 1;
    private static final int ST_COMPLETED = 2;
    private static final int ST_ERROR = 3;
    private Subscription subscription;
    private volatile Publisher<T> publisher;
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<JAsyncSubscriber, Publisher> PUBLISHER = AtomicReferenceFieldUpdater.newUpdater(JAsyncSubscriber.class, Publisher.class, "publisher");


    public JAsyncSubscriber(long initRequest) {
        this.initRequest = initRequest;
        this.cache = new UnPaddedLockFreeArrayQueue0<>(2);
        this.contexts = new UnPaddedLockFreeArrayQueue0<>(2);
        this.remain = initRequest;
        this.state = ST_READY;
    }

    @Override
    public void onSubscribe(Subscription s) {
        this.subscription = s;
        this.subscription.request(initRequest);
    }

    @Override
    public void onNext(T t) {
        PromiseContext<T> context = contexts.poll();
        if (context != null) {
            consume(context, t);
            return;
        }
        cache.offer(t);
        context = contexts.poll();
        if (context != null) {
            T v = cache.poll();
            if (v != null) {
                consume(context, v);
            }
        }
    }

    @Override
    public void onError(Throwable t) {
        if (STATE.compareAndSet(this, ST_READY, ST_ERROR)) {
            this.error = t;
            PromiseContext<T> context;
            while ((context = contexts.poll()) != null) {
                context.getThunk().reject(t, context.getContext());
            }
        }
    }

    @Override
    public void onComplete() {
        if (STATE.compareAndSet(this, ST_READY, ST_COMPLETED)) {
            PromiseContext<T> context;
            while ((context = contexts.poll()) != null) {
                context.getThunk().resolve(null, context.getContext());
            }
        }
    }

    public void addPromiseContext(PromiseContext<T> context) {
        T v = cache.poll();
        if (v != null) {
            consume(context, v);
            return;
        }
        contexts.offer(context);
        v = cache.poll();
        // v is not null means onNext is called.
        if (v != null) {
            PromiseContext<T> ctx = contexts.poll();
            if (ctx != null) {
                consume(ctx, v);
            }
        } else if (state == ST_COMPLETED) {
            context.getThunk().resolve(null, context.getContext());
        } else if (state == ST_ERROR) {
            context.getThunk().reject(error, context.getContext());
        }
    }

    public void cancel() {
        state = ST_CANCEL;
        subscription.cancel();
    }

    private void consume(PromiseContext<T> context, T value) {
        while (true) {
            long remain = this.remain;
            if (remain < 1) {
                throw new IllegalStateException("This is impossible.");
            }
            if (REMAIN.weakCompareAndSet(this, remain, remain - 1)) {
                context.getThunk().resolve(value, context.getContext());
                if (remain == 1) {
                    this.remain = context.getNextRequest();
                    subscription.request(this.remain);
                }
                return;
            }
        }
    }

    public void subscribe(Publisher<T> publisher) {
        if (!PUBLISHER.compareAndSet(this, null, publisher) && this.publisher != publisher) {
            throw new IllegalStateException("The subscriber has been subscribed by publisher " + this.publisher + ".");
        }
    }
}
