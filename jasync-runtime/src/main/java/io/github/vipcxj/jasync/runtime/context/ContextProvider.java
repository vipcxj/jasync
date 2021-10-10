package io.github.vipcxj.jasync.runtime.context;

import com.google.auto.service.AutoService;
import io.github.vipcxj.jasync.runtime.promise.ContextPromise;
import io.github.vipcxj.jasync.spec.JContext;
import io.github.vipcxj.jasync.spec.JPromise2;
import io.github.vipcxj.jasync.spec.spi.JContextProvider;

@AutoService(JContextProvider.class)
public class ContextProvider implements JContextProvider {

    @Override
    public JContext defaultContext() {
        return Context.DEFAULTS;
    }

    @Override
    public JPromise2<JContext> current() {
        return new ContextPromise();
    }

}
