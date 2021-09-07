package io.github.vipcxj.jasync.reactive;

import io.github.vipcxj.jasync.spec.Promise;
import io.github.vipcxj.jasync.spec.Utils;
import io.github.vipcxj.jasync.spec.functional.PromiseSupplier;
import io.github.vipcxj.jasync.spec.spi.PromiseProvider;
import reactor.core.publisher.Mono;

public class Promises implements PromiseProvider {

    public static <T> Promise<T> from(Mono<T> mono) {
        return new MonoPromise<>(mono);
    }

    public <T> Promise<T> just(T value) {
        return new MonoPromise<>(Mono.justOrEmpty(value));
    }

    public <T> Promise<T> defer(PromiseSupplier<T> block) {
        return from(Mono.defer(() -> {
            try {
                return Utils.safeGet(block).unwrap();
            } catch (Throwable t) {
                return Mono.error(t);
            }
        }));
    }

    @Override
    public <T> Promise<T> error(Throwable t) {
        return from(Mono.error(t));
    }
}
