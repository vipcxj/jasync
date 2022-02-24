import io.github.vipcxj.jasync.runtime.context.ContextProvider;
import io.github.vipcxj.jasync.runtime.promise.PromiseSupport;
import io.github.vipcxj.jasync.spec.spi.JContextProvider;
import io.github.vipcxj.jasync.spec.spi.JPromiseSupport;

module jasync.runtime {
    requires jasync.spec;
    provides JContextProvider with ContextProvider;
    provides JPromiseSupport with PromiseSupport;
}