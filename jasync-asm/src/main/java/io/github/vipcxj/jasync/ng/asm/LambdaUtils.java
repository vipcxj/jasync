package io.github.vipcxj.jasync.ng.asm;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;

import static io.github.vipcxj.jasync.ng.asm.Constants.*;

public class LambdaUtils {

    private final static String LAMBDA_META_FACTORY_META_FACTORY_DESC = "(" +
            "Ljava/lang/invoke/MethodHandles$Lookup;" +
            "Ljava/lang/String;" +
            "Ljava/lang/invoke/MethodType;" +
            "Ljava/lang/invoke/MethodType;" +
            "Ljava/lang/invoke/MethodHandle;" +
            "Ljava/lang/invoke/MethodType;" +
            ")Ljava/lang/invoke/CallSite;";

    private static Handle createMetaFactoryHandle() {
        return new Handle(
                Opcodes.H_INVOKESTATIC,
                "java/lang/invoke/LambdaMetafactory",
                "metafactory",
                LAMBDA_META_FACTORY_META_FACTORY_DESC,
                false
        );
    }

    private static Type[] prependType(Type[] types, Type type) {
        Type[] newTypes = new Type[types.length + 1];
        System.arraycopy(types, 0, newTypes, 1, types.length);
        newTypes[0] = type;
        return newTypes;
    }

    private static Type[] prependType(Type[] types, Type[] appends) {
        Type[] newTypes = new Type[types.length + appends.length];
        System.arraycopy(types, 0, newTypes, appends.length, types.length);
        System.arraycopy(appends, 0, newTypes, 0, appends.length);
        return newTypes;
    }

    public static InvokeDynamicInsnNode invokeLambda(
            String itfMethodName,
            Type itfDesc,
            Type itfMethodDesc,
            Type itfRealMethodDesc,
            Type ownerClass,
            String implMethodName,
            boolean isStatic,
            Type... extraArgs
    ) {
        Type itfCreatorDesc = isStatic ? Type.getMethodType(itfDesc, extraArgs) : Type.getMethodType(itfDesc, prependType(extraArgs, ownerClass));
        Handle handle = new Handle(
                isStatic ? Opcodes.H_INVOKESTATIC : Opcodes.H_INVOKESPECIAL,
                ownerClass.getInternalName(),
                implMethodName,
                Type.getMethodDescriptor(
                        itfRealMethodDesc.getReturnType(),
                        prependType(itfRealMethodDesc.getArgumentTypes(), extraArgs)
                ),
                false
        );
        return new InvokeDynamicInsnNode(
                itfMethodName,
                itfCreatorDesc.getDescriptor(),
                createMetaFactoryHandle(),
                itfMethodDesc,
                handle,
                itfRealMethodDesc
        );
    }

    public static InvokeDynamicInsnNode invokeJAsyncPromiseSupplier1(
            Type ownerClass,
            String implMethodName,
            boolean isStatic,
            Type... extraArgs
    ) {
        return invokeLambda(
                JASYNC_PROMISE_SUPPLIER1_METHOD_NAME,
                JASYNC_PROMISE_SUPPLIER1_DESC,
                JASYNC_PROMISE_SUPPLIER1_METHOD_DESC,
                JASYNC_PROMISE_SUPPLIER1_METHOD_DESC,
                ownerClass,
                implMethodName,
                isStatic,
                extraArgs
        );
    }

    public static InvokeDynamicInsnNode invokeJAsyncPromiseFunction3(
            Type ownerClass,
            String implMethodName,
            Type typeT,
            boolean isStatic,
            Type... extraArgs
    ) {
        return invokeLambda(
                JASYNC_PROMISE_FUNCTION3_METHOD_NAME,
                JASYNC_PROMISE_FUNCTION3_DESC,
                JASYNC_PROMISE_FUNCTION3_METHOD_DESC,
                Type.getMethodType(JPROMISE_DESC, typeT, THROWABLE_DESC, JCONTEXT_DESC),
                ownerClass,
                implMethodName,
                isStatic,
                extraArgs
        );
    }

    public static InvokeDynamicInsnNode invokeJAsyncCatchFunction1(
            Type ownerClass,
            String implMethodName,
            Type exceptionType,
            boolean isStatic,
            Type... extraArgs
    ) {
        return invokeLambda(
                JASYNC_CATCH_FUNCTION1_METHOD_NAME,
                JASYNC_CATCH_FUNCTION1_DESC,
                JASYNC_CATCH_FUNCTION1_METHOD_DESC,
                Type.getMethodType(JPROMISE_DESC, exceptionType, JCONTEXT_DESC),
                ownerClass,
                implMethodName,
                isStatic,
                extraArgs
        );
    }

    public static InvokeDynamicInsnNode invokeJAsyncPortalTask(
            Type ownerClass,
            String implMethodName,
            boolean isStatic,
            Type... extraArgs
    ) {
        return invokeLambda(
                JASYNC_PROMISE_FUNCTION1_METHOD_NAME,
                JASYNC_PROMISE_FUNCTION1_DESC,
                JASYNC_PROMISE_FUNCTION1_METHOD_DESC,
                Type.getMethodType(JPROMISE_DESC, AsmHelper.getArrayType(OBJECT_DESC, 1), JCONTEXT_DESC),
                ownerClass,
                implMethodName,
                isStatic,
                extraArgs
        );
    }
}
