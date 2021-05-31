package io.github.vipcxj.jasync.core.javac;

import io.github.vipcxj.jasync.spec.Promise;

public class Constants {
    public static final String PROMISE = Promise.class.getCanonicalName();
    public static final String DEFER_VOID = "deferVoid";
    public static final String PROMISE_DEFER_VOID = PROMISE + "." + DEFER_VOID;
    public static final String THEN_VOID = "thenVoid";

    public static final String REFERENCE_GET = "getValue";
    public static final String REFERENCE_ASSIGN = "setAndGet";
    public static final String REFERENCE_PRE_INC = "incrementAndGetValue";
    public static final String REFERENCE_PRE_DEC = "decrementAndGetValue";
    public static final String REFERENCE_POST_INC = "getValueAndIncrement";
    public static final String REFERENCE_POST_DEC = "getValueAndDecrement";
    public static final String REFERENCE_PLUS_ASSIGN = "addAndGetValue";
    public static final String REFERENCE_MINUS_ASSIGN = "minusAndGetValue";
    public static final String REFERENCE_MULTIPLY_ASSIGN = "mulAndGetValue";
    public static final String REFERENCE_DIVIDE_ASSIGN = "divideAndGetValue";
    public static final String REFERENCE_MOD_ASSIGN = "modAndGetValue";
    public static final String REFERENCE_LEFT_SHIFT_ASSIGN = "leftShiftAndGetValue";
    public static final String REFERENCE_RIGHT_SHIFT_ASSIGN = "rightShiftAndGetValue";
    public static final String REFERENCE_UNSIGNED_RIGHT_SHIFT_ASSIGN = "unsignedRightShiftAndGetValue";
    public static final String REFERENCE_LOGIC_AND_ASSIGN = "andAndGetValue";
    public static final String REFERENCE_LOGIC_OR_ASSIGN = "orAndGetValue";
    public static final String REFERENCE_LOGIC_XOR_ASSIGN = "xorAndGetValue";
}
