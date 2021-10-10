package io.github.vipcxj.jasync.spec;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface JHandle<T> extends JDisposable {
    JHandle<T> onSuccess(BiConsumer<T, JContext> consumer);
    default JHandle<T> onSuccess(Consumer<T> consumer) {
        return onSuccess((v, ctx) -> consumer.accept(v));
    }
    JHandle<T> onError(BiConsumer<Throwable, JContext> consumer);
    default JHandle<T> onError(Consumer<Throwable> consumer) {
        return onError((error, ctx) -> consumer.accept(error));
    }
    JHandle<T> onFinally(Consumer<JContext> runnable);
    default JHandle<T> onFinally(Runnable runnable) {
        return onFinally(ctx -> runnable.run());
    }
    JHandle<T> onDispose(Runnable runnable);
}
