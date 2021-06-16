package io.github.vipcxj.jasync.test.utils;

import io.github.vipcxj.jasync.reactive.Promises;
import io.github.vipcxj.jasync.spec.Promise;
import reactor.core.publisher.Mono;

public class PromiseFuns {

    Promise<Character> valueFun(char value) {
        return Promises.from(Mono.just(value));
    }

    Promise<Boolean> valueFun(boolean value) {
        return Promises.from(Mono.just(value));
    }

    Promise<Byte> valueFun(byte value) {
        return Promises.from(Mono.just(value));
    }

    Promise<Short> valueFun(short value) {
        return Promises.from(Mono.just(value));
    }

    Promise<Integer> valueFun(int value) {
        return Promises.from(Mono.just(value));
    }

    Promise<Long> valueFun(long value) {
        return Promises.from(Mono.just(value));
    }

    Promise<Float> valueFun(float value) {
        return Promises.from(Mono.just(value));
    }

    Promise<Double> valueFun(double value) {
        return Promises.from(Mono.just(value));
    }

}
