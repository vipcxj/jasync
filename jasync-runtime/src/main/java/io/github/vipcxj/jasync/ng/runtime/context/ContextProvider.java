package io.github.vipcxj.jasync.ng.runtime.context;

import com.google.auto.service.AutoService;
import io.github.vipcxj.jasync.ng.runtime.promise.ContextPromise;
import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JPushContext;
import io.github.vipcxj.jasync.ng.spec.spi.JContextProvider;

@AutoService(JContextProvider.class)
public class ContextProvider implements JContextProvider {

    @Override
    public JContext defaultContext() {
        return Context.DEFAULTS;
    }

    @Override
    public JPromise<JContext> current() {
        return new ContextPromise();
    }

    @Override
    public JPushContext createPushContext() {
        return new JPushContextImpl();
    }
}
