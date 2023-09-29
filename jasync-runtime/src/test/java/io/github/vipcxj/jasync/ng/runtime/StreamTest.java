package io.github.vipcxj.jasync.ng.runtime;

import io.github.vipcxj.jasync.ng.runtime.stream.Stream;
import io.github.vipcxj.jasync.ng.spec.AwaitType;
import io.github.vipcxj.jasync.ng.spec.JHandle;
import io.github.vipcxj.jasync.ng.spec.functional.JAsyncPromiseSupplier0;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class StreamTest {

    @Test
    public void testParallel() throws InterruptedException {
        Stream<Integer> stream = new Stream<>(1000);
        List<JHandle<Integer>> handles = new ArrayList<>();
        for (int i = 0; i < 1000000; ++i) {
            JHandle<Integer> handle = stream.produce(i)
                    .then(AwaitType.SCHEDULE, (JAsyncPromiseSupplier0<Integer>) stream::consume)
                    .async();
            handles.add(handle);
        }
        for (JHandle<Integer> handle : handles) {
            handle.block();
        }
        Assertions.assertEquals(0, stream.getSize());
    }
}
