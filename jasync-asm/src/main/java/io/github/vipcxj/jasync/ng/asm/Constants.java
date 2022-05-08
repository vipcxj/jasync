package io.github.vipcxj.jasync.ng.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class Constants {
    public static final String ANN_ASYNC_DESC = "Lio/github/vipcxj/jasync/ng/spec/annotations/Async;";
    public static final String NULL_NAME = "null";
    public static final Type NULL_DESC = Type.getObjectType(NULL_NAME);
    public static final String OBJECT_NAME = "java/lang/Object";
    public static final Type OBJECT_DESC = Type.getObjectType(OBJECT_NAME);
    public static final Type OBJECT_ARRAY_DESC = AsmHelper.getArrayType(OBJECT_DESC, 1);
    public static final String CLONEABLE_NAME = "java/lang/Cloneable";
    public static final Type CLONEABLE_DESC = Type.getObjectType(CLONEABLE_NAME);
    public static final String SERIALIZABLE_NAME = "java/lang/Serializable";
    public static final Type SERIALIZABLE_DESC = Type.getObjectType(SERIALIZABLE_NAME);
    public static final String THROWABLE_NAME = "java/lang/Throwable";
    public static final Type THROWABLE_DESC = Type.getObjectType(THROWABLE_NAME);
    public static final String JPROMISE_NAME = "io/github/vipcxj/jasync/ng/spec/JPromise";
    public static final Type JPROMISE_DESC = Type.getObjectType(JPROMISE_NAME);

    public static final String JASYNC_PROMISE_FUNCTION0_NAME = "io/github/vipcxj/jasync/ng/spec/functional/JAsyncPromiseFunction0";
    public static final Type JASYNC_PROMISE_FUNCTION0_DESC = Type.getObjectType(JASYNC_PROMISE_FUNCTION0_NAME);
    public static final String JASYNC_PROMISE_FUNCTION0_METHOD_NAME = "apply";
    public static final Type JASYNC_PROMISE_FUNCTION0_METHOD_DESC = Type.getMethodType(JPROMISE_DESC, OBJECT_DESC);

    public static final String JASYNC_PROMISE_FUNCTION2_NAME = "io/github/vipcxj/jasync/ng/spec/functional/JAsyncPromiseFunction2";
    public static final Type JASYNC_PROMISE_FUNCTION2_DESC = Type.getObjectType(JASYNC_PROMISE_FUNCTION2_NAME);
    public static final String JASYNC_PROMISE_FUNCTION2_METHOD_NAME = "apply";
    public static final Type JASYNC_PROMISE_FUNCTION2_METHOD_DESC = Type.getMethodType(JPROMISE_DESC, OBJECT_DESC, THROWABLE_DESC);

    public static final String JASYNC_CATCH_FUNCTION0_NAME = "io/github/vipcxj/jasync/ng/spec/functional/JAsyncCatchFunction0";
    public static final Type JASYNC_CATCH_FUNCTION0_DESC = Type.getObjectType(JASYNC_CATCH_FUNCTION0_NAME);
    public static final String JASYNC_CATCH_FUNCTION0_METHOD_NAME = "apply";
    public static final Type JASYNC_CATCH_FUNCTION0_METHOD_DESC = Type.getMethodType(JPROMISE_DESC, THROWABLE_DESC);

    public static final String JPROMISE_GEN_ID_NAME = "genId";
    public static final Type JPROMISE_GEN_ID_DESC = Type.getMethodType(Type.INT_TYPE);

    public static final String JPROMISE_PORTAL_NAME = "portal";
    public static final Type JPROMISE_PORTAL0_DESC = Type.getMethodType(JPROMISE_DESC, JASYNC_PROMISE_FUNCTION0_DESC, Type.INT_TYPE, OBJECT_ARRAY_DESC);

    public static final String JPROMISE_JUMP_NAME = "jump";
    public static final Type JPROMISE_JUMP_DESC = Type.getMethodType(JPROMISE_DESC, Type.INT_TYPE, OBJECT_ARRAY_DESC);

    public static final String JPROMISE_THEN_OR_CATCH_NAME = "thenOrCatch";
    public static final Type JPROMISE_THEN_OR_CATCH1_DESC = Type.getMethodType(JPROMISE_DESC, JASYNC_PROMISE_FUNCTION2_DESC);

    public static final String JPROMISE_DO_MULTI_CATCHES_NAME = "doMultiCatches";
    public static final Type JPROMISE_DO_MULTI_CATCHES1_DESC = Type.getMethodType(JPROMISE_DESC, OBJECT_ARRAY_DESC);

    public static final String AWAIT = "await";

    public static final String INTEGER_NAME = "java/lang/Integer";
    public static final Type INTEGER_DESC = Type.getObjectType(INTEGER_NAME);
    public static final String INTEGER_VALUE_OF_NAME = "valueOf";
    public static final Type INTEGER_VALUE_OF_DESC = Type.getMethodType(INTEGER_DESC, Type.INT_TYPE);
    public static final String INTEGER_INT_VALUE_NAME = "intValue";
    public static final Type INTEGER_INT_VALUE_DESC = Type.getMethodType(Type.INT_TYPE);

    public static final String FLOAT_NAME = "java/lang/Float";
    public static final Type FLOAT_DESC = Type.getObjectType(FLOAT_NAME);
    public static final String FLOAT_VALUE_OF_NAME = "valueOf";
    public static final Type FLOAT_VALUE_OF_DESC = Type.getMethodType(FLOAT_DESC, Type.FLOAT_TYPE);
    public static final String FLOAT_FLOAT_VALUE_NAME = "floatValue";
    public static final Type FLOAT_FLOAT_VALUE_DESC = Type.getMethodType(Type.FLOAT_TYPE);

    public static final String DOUBLE_NAME = "java/lang/Double";
    public static final Type DOUBLE_DESC = Type.getObjectType(DOUBLE_NAME);
    public static final String DOUBLE_VALUE_OF_NAME = "valueOf";
    public static final Type DOUBLE_VALUE_OF_DESC = Type.getMethodType(DOUBLE_DESC, Type.DOUBLE_TYPE);
    public static final String DOUBLE_DOUBLE_VALUE_NAME = "doubleValue";
    public static final Type DOUBLE_DOUBLE_VALUE_DESC = Type.getMethodType(Type.DOUBLE_TYPE);

    public static final String LONG_NAME = "java/lang/Long";
    public static final Type LONG_DESC = Type.getObjectType(LONG_NAME);
    public static final String LONG_VALUE_OF_NAME = "valueOf";
    public static final Type LONG_VALUE_OF_DESC = Type.getMethodType(LONG_DESC, Type.LONG_TYPE);
    public static final String LONG_LONG_VALUE_NAME = "longValue";
    public static final Type LONG_LONG_VALUE_DESC = Type.getMethodType(Type.LONG_TYPE);

    public static final String BOOLEAN_NAME = "java/lang/Boolean";
    public static final Type BOOLEAN_DESC = Type.getObjectType(BOOLEAN_NAME);
    public static final String BOOLEAN_VALUE_OF_NAME = "valueOf";
    public static final Type BOOLEAN_VALUE_OF_DESC = Type.getMethodType(BOOLEAN_DESC, Type.BOOLEAN_TYPE);
    public static final String BOOLEAN_BOOLEAN_VALUE_NAME = "booleanValue";
    public static final Type BOOLEAN_BOOLEAN_VALUE_DESC = Type.getMethodType(Type.BOOLEAN_TYPE);

    public static final String SHORT_NAME = "java/lang/Boolean";
    public static final Type SHORT_DESC = Type.getObjectType(SHORT_NAME);
    public static final String SHORT_VALUE_OF_NAME = "valueOf";
    public static final Type SHORT_VALUE_OF_DESC = Type.getMethodType(SHORT_DESC, Type.SHORT_TYPE);
    public static final String SHORT_SHORT_VALUE_NAME = "shortValue";
    public static final Type SHORT_SHORT_VALUE_DESC = Type.getMethodType(Type.SHORT_TYPE);

    public static final String CHARACTER_NAME = "java/lang/Character";
    public static final Type CHARACTER_DESC = Type.getObjectType(CHARACTER_NAME);
    public static final String CHARACTER_VALUE_OF_NAME = "valueOf";
    public static final Type CHARACTER_VALUE_OF_DESC = Type.getMethodType(CHARACTER_DESC, Type.CHAR_TYPE);
    public static final String CHARACTER_CHAR_VALUE_NAME = "charValue";
    public static final Type CHARACTER_CHAR_VALUE_DESC = Type.getMethodType(Type.CHAR_TYPE);

    public static final String BYTE_NAME = "java/lang/Byte";
    public static final Type BYTE_DESC = Type.getObjectType(BYTE_NAME);
    public static final String BYTE_VALUE_OF_NAME = "valueOf";
    public static final Type BYTE_VALUE_OF_DESC = Type.getMethodType(BYTE_DESC, Type.BYTE_TYPE);
    public static final String BYTE_BYTE_VALUE_NAME = "byteValue";
    public static final Type BYTE_BYTE_VALUE_DESC = Type.getMethodType(Type.BYTE_TYPE);

    public static final int ASM_VERSION = Opcodes.ASM9;

}
