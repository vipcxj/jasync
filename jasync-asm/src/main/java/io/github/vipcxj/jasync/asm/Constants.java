package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class Constants {
    public static final String ANN_ASYNC_DESC = "Lio/github/vipcxj/jasync/spec/annotations/Async;";
    public static final String OBJECT_NAME = "java/lang/Object";
    public static final Type OBJECT_DESC = Type.getObjectType(OBJECT_NAME);
    public static final String THROWABLE_NAME = "java/lang/Throwable";
    public static final String JPROMISE_NAME = "io/github/vipcxj/jasync/spec/JPromise2";
    public static final Type JPROMISE_DESC = Type.getObjectType(JPROMISE_NAME);
    public static final String JCONTEXT_NAME = "io/github/vipcxj/jasync/spec/JContext";
    public static final Type JCONTEXT_DESC = Type.getObjectType(JCONTEXT_NAME);
    public static final String JPORTAL_NAME = "io/github/vipcxj/jasync/spec/JPortal";
    public static final Type JPORTAL_DESC = Type.getObjectType(JPORTAL_NAME);
    public static final String JPORTAL_JUMP_NAME = "jump";
    public static final Type JPORTAL_JUMP_DESC = Type.getMethodType(JPROMISE_DESC);
    public static final String JPORTAL_TASK_NAME = "io/github/vipcxj/jasync/spec/functional/JAsyncPortalTask";
    public static final Type JPORTAL_TASK_DESC = Type.getObjectType(JPORTAL_TASK_NAME);
    public static final String JPORTAL_TASK_INVOKE_NAME = "invoke";
    public static final Type JPORTAL_TASK_INVOKE_DESC = Type.getMethodType(JPROMISE_DESC, JPORTAL_DESC, JCONTEXT_DESC);
    public static final String JPROMISE_PORTAL_NAME = "portal";
    public static final Type JPROMISE_PORTAL_DESC = Type.getMethodType(JPROMISE_DESC, JPORTAL_TASK_DESC);
    public static final String JASYNC_PROMISE_FUNCTION0_NAME = "io/github/vipcxj/jasync/spec/functional/JAsyncPromiseFunction0";
    public static final Type JASYNC_PROMISE_FUNCTION0_DESC = Type.getObjectType(JASYNC_PROMISE_FUNCTION0_NAME);
    public static final String JASYNC_PROMISE_FUNCTION0_METHOD_NAME = "apply";
    public static final Type JASYNC_PROMISE_FUNCTION0_METHOD_DESC = Type.getMethodType(JPROMISE_DESC, OBJECT_DESC);
    public static final String JPROMISE_THEN_NAME = "then";
    public static final Type JPROMISE_THEN_DESC = Type.getMethodType(JPROMISE_DESC, JASYNC_PROMISE_FUNCTION0_DESC);
    public static final String JASYNC_PROMISE_FUNCTION1_NAME = "io/github/vipcxj/jasync/spec/functional/JAsyncPromiseFunction1";
    public static final Type JASYNC_PROMISE_FUNCTION1_DESC = Type.getObjectType(JASYNC_PROMISE_FUNCTION1_NAME);
    public static final String JASYNC_PROMISE_FUNCTION1_METHOD_NAME = "apply";
    public static final Type JASYNC_PROMISE_FUNCTION1_METHOD_DESC = Type.getMethodType(JPROMISE_DESC, OBJECT_DESC, JCONTEXT_DESC);
    public static final String JPROMISE_THEN_WITH_CONTEXT_NAME = "thenWithContext";
    public static final Type JPROMISE_THEN_WITH_CONTEXT_DESC = Type.getMethodType(JPROMISE_DESC, JASYNC_PROMISE_FUNCTION1_DESC);
    public static final String AWAIT = "await";
    public static final int ASM_VERSION = Opcodes.ASM9;

    public static final Label LABEL_START = new Label();
    public static final Label LABEL_END = new Label();
}
