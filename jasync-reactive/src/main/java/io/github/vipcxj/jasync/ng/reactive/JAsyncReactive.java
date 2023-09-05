package io.github.vipcxj.jasync.ng.reactive;

import org.reactivestreams.Publisher;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class JAsyncReactive {
    
    public static <T> Publisher<T> toPublisher(JPromise<T> promise) {
        return new JAsyncPublisher<>(promise);
    }

    public static <T> Mono<T> toMono(JPromise<T> promise) {
        return Mono.from(toPublisher(promise));
    }

    public static <T> Flux<T> toFlux(JPromise<T> promise) {
        return Flux.from(toPublisher(promise));
    }

    public static <T> JPromise<T> fromPublisher(Publisher<T> publisher) {
        JAsyncSubscriber<T> subscriber = new JAsyncSubscriber<>();
        publisher.subscribe(subscriber);
        return subscriber.createSubscriptionPromise().then(s -> {
            s.request(Long.MAX_VALUE);
            return subscriber.createValuePromise().then(r -> {
                if (r.getThrowable() == null) {
                    return JPromise.just(r.getValue());
                } else {
                    return JPromise.error(r.getThrowable());
                }
            });
        });
    }
}
