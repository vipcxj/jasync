package io.github.vipcxj.jasync.ng.spring;

import java.util.concurrent.TimeUnit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.github.vipcxj.jasync.ng.spec.JPromise;

@RestController
public class JAsyncController {

    @GetMapping("/hello-world")
    public JPromise<String> sayHello() {
        return JPromise.just("Hello World!");
    }
    
    @GetMapping("/sleep")
    public JPromise<Void> sleep() {
        return JPromise.sleep(1, TimeUnit.SECONDS);
    }

    @PostMapping("/echo")
    public JPromise<String> echo(@RequestBody JPromise<String> msg) {
        return msg;
    }
}
