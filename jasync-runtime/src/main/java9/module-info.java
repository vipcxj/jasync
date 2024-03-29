import io.github.vipcxj.jasync.ng.runtime.context.ContextProvider;
import io.github.vipcxj.jasync.ng.runtime.promise.PromiseSupport;
import io.github.vipcxj.jasync.ng.runtime.schedule.SchedulerSupport;
import io.github.vipcxj.jasync.ng.runtime.stream.StreamSupport;
import io.github.vipcxj.jasync.ng.spec.spi.JContextProvider;
import io.github.vipcxj.jasync.ng.spec.spi.JPromiseSupport;
import io.github.vipcxj.jasync.ng.spec.spi.JSchedulerSupport;
import io.github.vipcxj.jasync.ng.spec.spi.JStreamSupport;

module jasync.runtime {
    requires jasync.spec;
    provides JContextProvider with ContextProvider;
    provides JPromiseSupport with PromiseSupport;
    provides JSchedulerSupport with SchedulerSupport;
    provides JStreamSupport with StreamSupport;
}