package io.github.vipcxj.jasync.ng.reactive;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JPromiseTrigger;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class JAsyncReactiveTest {
    

    @Test
    void testToPublisher() {
        JPromise<String> msg = JPromise.just("test");
        Publisher<String> publisher = JAsyncReactive.toPublisher(msg);
        StepVerifier.create(publisher)
            .expectNext("test")
            .expectComplete()
            .verify(Duration.ofSeconds(1));

        msg = JPromise.empty();
        publisher = JAsyncReactive.toPublisher(msg);
        StepVerifier.create(publisher)
            .expectNext((String) null)
            .expectComplete()
            .verify(Duration.ofSeconds(1));

        msg = JPromise.error(new RuntimeException("test"));
        publisher = JAsyncReactive.toPublisher(msg);
        StepVerifier.create(publisher)
            .expectErrorSatisfies(error -> {
                Assertions.assertInstanceOf(RuntimeException.class, error);
                Assertions.assertEquals("test", error.getMessage());
            })
            .verify(Duration.ofSeconds(1));

        msg = JPromise.sleep(300, TimeUnit.MILLISECONDS).thenReturn("test");
        publisher = JAsyncReactive.toPublisher(msg);
        StepVerifier.create(publisher)
            .expectNext("test")
            .expectComplete()
            .verify(Duration.ofSeconds(1));
        JPromiseTrigger<String> trigger = JPromise.createTrigger();

        publisher = JAsyncReactive.toPublisher(trigger.getPromise());
        StepVerifier verifier = StepVerifier.create(publisher)
            .expectNext("test")
            .expectComplete()
            .verifyLater();
        trigger.resolve("test");
        verifier.verify(Duration.ofSeconds(1));
    }

    @Test
    void testFromPublisher() {
        Mono<String> mono = Mono.just("test");
        JPromise<String> promise = JAsyncReactive.fromPublisher(mono);
        promise.onFinally((v, t) -> {
            Assertions.assertNull(t);
            Assertions.assertEquals("test", v);
        }).async();

        mono = Mono.defer(() -> Mono.just("test"));
        promise = JAsyncReactive.fromPublisher(mono);
        promise.onFinally((v, t) -> {
            Assertions.assertNull(t);
            Assertions.assertEquals("test", v);
        }).async();

        mono = Mono.error(new RuntimeException("test"));
        promise = JAsyncReactive.fromPublisher(mono);
        promise.onFinally((v, t) -> {
            Assertions.assertInstanceOf(RuntimeException.class, t);
            Assertions.assertEquals("test", t.getMessage());
            Assertions.assertNull(null, v);
        }).async();
    }
}
