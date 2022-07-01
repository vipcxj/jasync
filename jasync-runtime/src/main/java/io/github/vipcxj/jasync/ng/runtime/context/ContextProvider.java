package io.github.vipcxj.jasync.ng.runtime.context;

import com.google.auto.service.AutoService;
import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JScheduler;
import io.github.vipcxj.jasync.ng.spec.spi.JContextProvider;

@AutoService(JContextProvider.class)
public class ContextProvider implements JContextProvider {

    @Override
    public JContext create(JScheduler scheduler, boolean supportStackTrace) {
        return new Context(scheduler, supportStackTrace);
    }
}
