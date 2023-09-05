package io.github.vipcxj.jasync.ng.spring;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.vipcxj.jasync.ng.spec.JPromise;

@RestController
public class JAsyncController {

    @GetMapping("/hello-world")
    public JPromise<String> sayHello() {
        return JPromise.just("Hello World!");
    }
    
}
