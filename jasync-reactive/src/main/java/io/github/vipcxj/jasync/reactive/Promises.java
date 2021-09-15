package io.github.vipcxj.jasync.reactive;

import io.github.vipcxj.jasync.spec.JPromise;
import io.github.vipcxj.jasync.spec.Utils;
import io.github.vipcxj.jasync.spec.functional.PromiseSupplier;
import io.github.vipcxj.jasync.spec.spi.PromiseProvider;
import reactor.core.publisher.Mono;

public class Promises implements PromiseProvider {

    public static <T> JPromise<T> from(Mono<T> mono) {
        return new ReactorPromise<>(mono);
    }

    public <T> JPromise<T> just(T value) {
        return new ReactorPromise<>(Mono.justOrEmpty(value));
    }

    public <T> JPromise<T> defer(PromiseSupplier<T> block) {
        return from(Mono.defer(() -> {
            try {
                return Utils.safeGet(block).unwrap(Mono.class);
            } catch (Throwable t) {
                return Mono.error(t);
            }
        }));
    }

    @Override
    public <T> JPromise<T> error(Throwable t) {
        return from(Mono.error(t));
    }
}
