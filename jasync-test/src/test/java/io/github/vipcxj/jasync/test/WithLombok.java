package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.spec.JAsync;
import io.github.vipcxj.jasync.spec.JPromise;
import io.github.vipcxj.jasync.spec.annotations.Async;
import lombok.Data;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Optional;

public class WithLombok {

    @Data
    public static class LombokObject {
        private boolean lombok;
        private ArrayList<Integer> list = new ArrayList<>();
    }

    private final LombokObject object;
    private int sum;

    public WithLombok() {
        this.object = new LombokObject();
        this.object.setLombok(true);
        this.object.getList().add(1);
        this.object.getList().add(2);
        this.object.getList().add(3);
        Optional
                .ofNullable(this.object.getList())
                .ifPresent(list -> sum = list.stream().mapToInt(i -> i).sum());
    }

    @Async
    public JPromise<Boolean> isLombok() {
        return JAsync.just(object.isLombok());
    }

    @Test
    public void test() {
        Assertions.assertEquals(object.isLombok(), isLombok().block());
        Assertions.assertEquals(6, sum);
    }
}
