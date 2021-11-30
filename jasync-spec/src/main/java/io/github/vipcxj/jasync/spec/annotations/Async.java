package io.github.vipcxj.jasync.spec.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD})
public @interface Async {

    int BYTE_CODE_OPTION_OFF = 0;
    int BYTE_CODE_OPTION_ON = 1;
    int BYTE_CODE_OPTION_FRAME = 2;
    int BYTE_CODE_OPTION_INDEX = 4;
    int BYTE_CODE_OPTION_FULL_SUPPORT = BYTE_CODE_OPTION_ON | BYTE_CODE_OPTION_FRAME | BYTE_CODE_OPTION_INDEX;

    String debugId() default "";
    boolean disabled() default false;
    boolean logResultTree() default false;
    boolean logResultPosTree() default false;
    int logOriginalByteCode() default BYTE_CODE_OPTION_OFF;
    int logResultByteCode() default BYTE_CODE_OPTION_OFF;
    boolean logOriginalAsm() default false;
    boolean logResultAsm() default false;
    boolean verify() default false;
    boolean experiment() default false;
    boolean debug() default false;
    Method method() default Method.AUTO;

    enum Method {
        AUTO, INDY, INNER_CLASS
    }
}
