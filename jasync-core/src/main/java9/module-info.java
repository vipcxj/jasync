import io.github.vipcxj.jasync.ng.core.JAsyncProcessor;

import javax.annotation.processing.Processor;

module jasync.core {
    requires jdk.compiler;
    provides Processor with JAsyncProcessor;
}