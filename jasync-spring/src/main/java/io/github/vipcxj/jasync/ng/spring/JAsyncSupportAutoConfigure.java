package io.github.vipcxj.jasync.ng.spring;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ReactiveTypeDescriptor;

import io.github.vipcxj.jasync.ng.reactive.JAsyncReactive;
import io.github.vipcxj.jasync.ng.spec.JPromise;

@Configuration
public class JAsyncSupportAutoConfigure {

    @PostConstruct
    public void doConfigure(ReactiveAdapterRegistry registry) {
        registry.registerReactiveType(
            ReactiveTypeDescriptor.singleOptionalValue(JPromise.class, JPromise::empty),
            (s) -> JAsyncReactive.toPublisher((JPromise<?>) s),
            (p) -> JAsyncReactive.fromPublisher(p)
        );
    }
}
