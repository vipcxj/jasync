package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;

import static io.github.vipcxj.jasync.asm.Constants.*;

public class LambdaUtils {

    private final static String LAMBDA_META_FACTORY_META_FACTORY_DESC = "(" +
            "Ljava/lang/invoke/MethodHandles$Lookup;" +
            "Ljava/lang/String;" +
            "Ljava/lang/invoke/MethodType;" +
            "Ljava/lang/invoke/MethodType;" +
            "Ljava/lang/invoke/MethodHandle;" +
            "Ljava/lang/invoke/MethodType;" +
            ")Ljava/lang/invoke/CallSite;";

    private final static Handle META_FACTORY_HANDLE = new Handle(
            Opcodes.H_INVOKESTATIC,
            "java/lang/invoke/LambdaMetafactory",
            "metafactory",
            LAMBDA_META_FACTORY_META_FACTORY_DESC,
            false
    );

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

    private static Type[] prependType(Type[] types, Type[] appends, Type append) {
        Type[] newTypes = new Type[types.length + appends.length + 1];
        System.arraycopy(types, 0, newTypes, appends.length + 1, types.length);
        System.arraycopy(appends, 0, newTypes, 1, appends.length);
        newTypes[0] = append;
        return newTypes;
    }

    public static InvokeDynamicInsnNode invokeLambda(
            String itfMethodName,
            Type itfDesc,
            Type itfMethodDesc,
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
                        itfMethodDesc.getReturnType(),
                        isStatic ? prependType(itfMethodDesc.getArgumentTypes(), extraArgs, ownerClass) : prependType(itfMethodDesc.getArgumentTypes(), extraArgs)
                ),
                false
        );
        return new InvokeDynamicInsnNode(
                itfMethodName,
                itfCreatorDesc.getDescriptor(),
                META_FACTORY_HANDLE,
                itfMethodDesc.getDescriptor(),
                handle,
                itfMethodDesc.getDescriptor()
        );
    }

    public static InvokeDynamicInsnNode invokeJAsyncPromiseFunction0(
            Type ownerClass,
            String implMethodName,
            boolean isStatic,
            Type... extraArgs
    ) {
        return invokeLambda(
                JASYNC_PROMISE_FUNCTION0_METHOD_NAME,
                JASYNC_PROMISE_FUNCTION0_DESC,
                JASYNC_PROMISE_FUNCTION0_METHOD_DESC,
                ownerClass,
                implMethodName,
                isStatic,
                extraArgs
        );
    }
}
