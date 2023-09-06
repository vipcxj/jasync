package io.github.vipcxj.jasync.ng.spring;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

@ExtendWith(SpringExtension.class)
@WebFluxTest(JAsyncController.class)
@ImportAutoConfiguration(JAsyncSupportAutoConfigure.class)
public class JAsyncControllerTest {
    
    @Autowired
    private WebTestClient webClient;


    @Test
    public void testSayHello() {
        webClient
            .get().uri("/hello-world")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .isEqualTo("Hello World!");
    }

    @Test
    public void testSleep() {
        webClient
            .mutateWith((b, hb, c) -> {
                b.responseTimeout(Duration.ofSeconds(2));
            })
            .get().uri("/sleep")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(Void.class)
            .isEqualTo(null);
    }

    @Test
    public void testEcho() {
        webClient
            .mutateWith((b, hb, c) -> {
                b.responseTimeout(Duration.ofDays(2));
            })
            .post().uri("/echo")
            .bodyValue("hi!")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .isEqualTo("hi!");
    }
}
