package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.spec.JAsync;
import io.github.vipcxj.jasync.spec.JPromise;
import io.github.vipcxj.jasync.spec.annotations.Async;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Issues {

    @Async(logResultTree = true)
    public <G> JPromise<G> get(Class<G> format) {
        Long data = JAsync.just(1L).await();
        return JAsync.just(format.cast(data));
    }

    @Test
    public void issue4() {
        Assertions.assertEquals(1L, get(Number.class).block());
    }
}
