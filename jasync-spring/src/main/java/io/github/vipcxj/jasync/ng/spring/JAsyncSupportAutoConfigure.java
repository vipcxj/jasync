package io.github.vipcxj.jasync.ng.spring;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ReactiveTypeDescriptor;

import io.github.vipcxj.jasync.ng.reactive.JAsyncReactive;
import io.github.vipcxj.jasync.ng.spec.JPromise;

@Configuration
public class JAsyncSupportAutoConfigure implements InitializingBean {

    @Autowired
    private ReactiveAdapterRegistry registry;

    @Override
    @SuppressWarnings("unchecked")
    public void afterPropertiesSet() throws Exception {
        try {
            Method registerReactiveTypeOverride = ReactiveAdapterRegistry.class.getMethod("registerReactiveTypeOverride", ReactiveTypeDescriptor.class, Function.class, Function.class);
            registerReactiveTypeOverride.invoke(
                this.registry,
                ReactiveTypeDescriptor.singleOptionalValue(JPromise.class, JPromise::empty),
                (Function<Object, Publisher<?>>) (s) -> JAsyncReactive.toPublisher((JPromise<?>) s),
                (Function<Publisher<?>, Object>) (p) -> JAsyncReactive.fromPublisher(p)
            );
        } catch (Throwable ignored) {
            registry.registerReactiveType(
                ReactiveTypeDescriptor.singleOptionalValue(JPromise.class, JPromise::empty),
                (s) -> JAsyncReactive.toPublisher((JPromise<?>) s),
                (p) -> JAsyncReactive.fromPublisher(p)
            );
            Field field = ReactiveAdapterRegistry.class.getDeclaredField("adapters");
            field.setAccessible(true);
            List<ReactiveAdapter> adapters = (List<ReactiveAdapter>) field.get(registry);
            ReactiveAdapter last = adapters.get(adapters.size() - 1);
            adapters.remove(adapters.size() - 1);
            adapters.add(0, last);
        }
    }
}
