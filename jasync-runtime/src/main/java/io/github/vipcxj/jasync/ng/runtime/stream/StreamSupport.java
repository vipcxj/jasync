package io.github.vipcxj.jasync.ng.runtime.stream;

import com.google.auto.service.AutoService;
import io.github.vipcxj.jasync.ng.spec.JStream;
import io.github.vipcxj.jasync.ng.spec.spi.JStreamSupport;

@AutoService(JStreamSupport.class)
public class StreamSupport implements JStreamSupport {

    @Override
    public <T> JStream<T> create(int capacity) {
        return new Stream<>(capacity);
    }
}
