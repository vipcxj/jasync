package io.github.vipcxj.jasync.ng.test;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class AnnotationTest {
    
    public JPromise<String> parametersWithAnnotations(@Ann1 String param1, String param2, @Ann2 String param3) {
        String p1 = JPromise.just(param1).await();
        String p2 = JPromise.just(param2).await();
        String p3 = JPromise.just(param3).await();
        return JPromise.just(p1 + p2 + p3);
    }

    @Test
    public void testParametersWithAnnotations() throws InterruptedException {
        String res = parametersWithAnnotations("hello", " ", "world").block();
        Assertions.assertEquals("hello world", res);
    }

    public @interface Ann1 {

    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ann2 {

    }

}
