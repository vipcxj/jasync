package io.github.vipcxj.jasync.spec.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.LOCAL_VARIABLE})
public @interface Async {
    String debugId() default "";
    boolean disabled() default false;
    boolean logResultTree() default false;
    boolean logResultPosTree() default false;
    boolean experiment() default false;
    boolean debug() default false;
    Method method() default Method.AUTO;


    enum Method {
        AUTO, INDY, INNER_CLASS
    }
}
