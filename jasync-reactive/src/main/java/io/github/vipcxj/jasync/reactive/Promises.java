package io.github.vipcxj.jasync.reactive;

import io.github.vipcxj.jasync.spec.Promise;
import io.github.vipcxj.jasync.spec.spi.PromiseProvider;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

public class Promises implements PromiseProvider {

    public static <T> Promise<T> from(Mono<T> mono) {
        return new MonoPromise<>(mono);
    }

    public <T> Promise<T> just(T value) {
        return new MonoPromise<>(Mono.justOrEmpty(value));
    }

    public <T> Promise<T> defer(Supplier<Promise<T>> block) {
        return from(Mono.defer(() -> {
            Promise<T> promise = block.get();
            return promise != null ? promise.unwrap() : Mono.empty();
        }));
    }

    @Override
    public <T> Promise<T> error(Throwable t) {
        return from(Mono.error(t));
    }
}
