package io.github.vipcxj.jasync.ng.spring;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ReactiveTypeDescriptor;

import io.github.vipcxj.jasync.ng.reactive.JAsyncReactive;
import io.github.vipcxj.jasync.ng.spec.JPromise;

@Configuration
public class JAsyncSupportAutoConfigure implements InitializingBean {

    @Autowired
    private ReactiveAdapterRegistry registry;

    @Override
    public void afterPropertiesSet() throws Exception {
        registry.registerReactiveType(
            ReactiveTypeDescriptor.singleOptionalValue(JPromise.class, JPromise::empty),
            (s) -> JAsyncReactive.toPublisher((JPromise<?>) s),
            (p) -> JAsyncReactive.fromPublisher(p)
        );
    }
}
