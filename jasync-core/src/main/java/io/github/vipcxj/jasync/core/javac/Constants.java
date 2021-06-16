package io.github.vipcxj.jasync.core.javac;

import io.github.vipcxj.jasync.spec.JAsync;
import io.github.vipcxj.jasync.spec.Promise;

public class Constants {
    public static final String PROMISE = Promise.class.getCanonicalName();
    public static final String THEN = "then";
    public static final String THEN_VOID = "thenVoid";
    public static final String JASYNC = JAsync.class.getCanonicalName();
    public static final String JASYNC_JUST = JASYNC + ".just";
    public static final String JASYNC_DEFER_VOID = JASYNC + ".deferVoid";
    public static final String JASYNC_DO_RETURN = JASYNC + ".doReturn";
    public static final String JASYNC_DO_BREAK = JASYNC + ".doBreak";
    public static final String JASYNC_DO_SWITCH = JASYNC + ".doSwitch";
    public static final String JASYNC_DO_WHILE = JASYNC + ".doWhile";
    public static final String JASYNC_DO_DO_WHILE = JASYNC + ".doDoWhile";
    public static final String JASYNC_DO_FOR_EACH_OBJECT = JASYNC + ".doForEachObject";
    public static final String JASYNC_DO_FOR_EACH_BOOLEAN = JASYNC + ".doForEachBoolean";
    public static final String JASYNC_DO_FOR_EACH_BYTE = JASYNC + ".doForEachByte";
    public static final String JASYNC_DO_FOR_EACH_CHAR = JASYNC + ".doForEachChar";
    public static final String JASYNC_DO_FOR_EACH_SHORT = JASYNC + ".doForEachShort";
    public static final String JASYNC_DO_FOR_EACH_INT = JASYNC + ".doForEachInt";
    public static final String JASYNC_DO_FOR_EACH_LONG = JASYNC + ".doForEachLong";
    public static final String JASYNC_DO_FOR_EACH_FLOAT = JASYNC + ".doForEachFloat";
    public static final String JASYNC_DO_FOR_EACH_DOUBLE = JASYNC + ".doForEachDouble";

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

    public static final String CASES = "io.github.vipcxj.jasync.spec.switchexpr.Cases";
    public static final String CASES_OF = CASES + ".of";
    public static final String INT_CASE = "io.github.vipcxj.jasync.spec.switchexpr.IntCase";
    public static final String INT_CASE_OF = INT_CASE + ".of";
    public static final String STRING_CASE = "io.github.vipcxj.jasync.spec.switchexpr.StringCase";
    public static final String STRING_CASE_OF = STRING_CASE + ".of";
    public static final String ENUM_CASE = "io.github.vipcxj.jasync.spec.switchexpr.EnumCase";
    public static final String ENUM_CASE_OF = ENUM_CASE + ".of";
}
