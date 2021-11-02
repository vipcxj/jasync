package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Value;

import java.util.ArrayList;
import java.util.List;

public class AwaitCodePiece extends CodePiece {

    private final Frame<? extends BasicValue> frame;
    private int locals;
    private int maxLocals;
    private final List<Type> arguments;
    private final MethodNode lambdaNode;
    private InsnList result;

    protected AwaitCodePiece(MethodContext methodContext, CodePiece parent, int from, int to) {
        super(methodContext, parent, from, to);
        assert from > 0;
        this.frame = methodContext.getFrames()[from - 1];
        assert frame != null;
        this.locals = calcValidLocals(frame);
        this.arguments = calcArgumentsType();
        int access = Opcodes.ACC_PRIVATE;
        if (methodContext.isStatic()) {
            access |= Opcodes.ACC_STATIC;
        }
        this.lambdaNode = new MethodNode(
                Constants.ASM_VERSION,
                access,
                methodContext.nextLambdaName(),
                Type.getMethodDescriptor(Constants.JPROMISE_DESC, arguments.toArray(new Type[0])),
                null,
                new String[] { Constants.THROWABLE_NAME }
        );
    }

    @Override
    public InsnList transform() {
        result = new InsnList();
        // store the current stack to the locals (offset by locals). the first one (index of locals) should be the promise
        // stack: a, b, promise | locals: x, y, z -> locals: x, y, z, promise, b, a
        storeStackToLocal();
        // push the target promise to stack
        // locals: x, y, z, promise, b, a -> stack: promise
        result.add(new VarInsnNode(Opcodes.ALOAD, locals));
        // push the previous locals to the stack
        // locals: x, y, z, promise, b, a -> stack: promise, x, y, z
        for (int i = 0; i < locals;) {
            BasicValue value = frame.getLocal(i);
            if (value != null && value.getType() != null) {
                Type type = value.getType();
                result.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), i));
                i += type.getSize();
            } else {
                result.add(new InsnNode(Opcodes.ACONST_NULL));
                ++i;
            }
        }
        // push the previous stack from locals to the stack, except the previous stack top, which is the promise.
        // locals: x, y, z, promise, b, a -> stack: promise, x, y, z, a, b
        int stackSize = frame.getStackSize();
        for (int i = 0, iLocal = maxLocals; i < stackSize - 1; ++i) {
            BasicValue value = frame.getStack(i);
            Type type = value.getType();
            if (type != null) {
                iLocal -= type.getSize();
                result.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), iLocal));
            } else {
                --iLocal;
                result.add(new VarInsnNode(Opcodes.ALOAD, iLocal));
            }
        }
        result.add(LambdaUtils.invokeJAsyncPromiseFunction0(
                methodContext.classType(),
                lambdaNode.name,
                methodContext.isStatic(),
                arguments.subList(0, arguments.size() - 2).toArray(new Type[arguments.size() - 2])
        ));
        result.add(new InsnNode(Opcodes.ARETURN));
        buildLambda();
        return result;
    }

    private List<Type> calcArgumentsType() {
        // stack: a, b, promise | locals: x, y, z
        List<Type> arguments = new ArrayList<>();
        // locals: x, y, z -> arguments
        for (int i = 0; i < locals;) {
            BasicValue value = frame.getLocal(i);
            Type type = value.getType();
            if (type != null) {
                arguments.add(type);
                i += type.getSize();
            } else {
                arguments.add(Constants.OBJECT_DESC);
                ++i;
            }
        }
        int stackSize = frame.getStackSize();
        // stack: a, b -> arguments
        for (int i = 0; i < stackSize - 1; ++i) {
            BasicValue value = frame.getStack(i);
            Type type = value.getType();
            if (type != null) {
                arguments.add(type);
            } else {
                arguments.add(Constants.OBJECT_DESC);
            }
        }
        Frame<? extends BasicValue> nextFrame = methodContext.getFrames()[from];
        // await type -> arguments
        BasicValue awaitValue = nextFrame.getStack(nextFrame.getStackSize() - 1);
        Type type = awaitValue.getType();
        if (type != null) {
            arguments.add(type);
        } else {
            arguments.add(Constants.OBJECT_DESC);
        }
        // context -> arguments
        arguments.add(Constants.JCONTEXT_DESC);
        // x, y, z, a, b, await type, context
        return arguments;
    }

    private static int calcValidLocals(Frame<?> frame) {
        int locals = frame.getLocals();
        int valid = -1;
        for (int i = 0; i < locals; ++i) {
            Value value = frame.getLocal(i);
            if (value == null) {
                if (valid == -1) {
                    valid = i;
                }
            } else {
                if (valid != -1) {
                    valid = -1;
                }
            }
        }
        return valid == -1 ? locals : valid;
    }

    private void storeStackToLocal() {
        int iLocal = this.locals;
        int stackSize = frame.getStackSize();
        for (int i = stackSize - 1; i >= 0; --i) {
            BasicValue value = frame.getStack(i);
            Type type = value.getType();
            if (type != null) {
                result.add(new VarInsnNode(type.getOpcode(Opcodes.ISTORE), iLocal));
                iLocal += type.getSize();
            } else {
                result.add(new VarInsnNode(Opcodes.ASTORE, iLocal));
                iLocal += 1;
            }
        }
        this.maxLocals = iLocal;
    }

    private void buildLambda() {
        lambdaNode.visitCode();
        // restore stack.
        // arguments: x, y, z, a, b, await type, context -> stack: a, b, await result
        Frame<? extends BasicValue> nextFrame = methodContext.getFrames()[from];
        int stackSize = nextFrame.getStackSize();
        for (int i = 0, j = locals; i < stackSize; ++i) {
            BasicValue value = nextFrame.getStack(i);
            Type type = value.getType();
            if (type != null) {
                lambdaNode.visitVarInsn(type.getOpcode(Opcodes.ILOAD), j);
                j += type.getSize();
            } else {
                lambdaNode.visitInsn(Opcodes.ACONST_NULL);
                ++j;
            }
        }
        for (int i = 1; i < insnNodes.size(); ++i) {
            InsnWithIndex insnWithIndex = insnNodes.get(i);
            AbstractInsnNode node = insnWithIndex.getInsnNode();
            if (node instanceof CodePieceInsnNode) {
                CodePieceInsnNode codePieceInsnNode = (CodePieceInsnNode) node;
                CodePiece codePiece = codePieceInsnNode.getCodePiece();
                lambdaNode.instructions.add(codePiece.transform());
            } else {
                lambdaNode.instructions.add(methodContext.cloneInsn(node));
            }
        }
        lambdaNode.visitMaxs(0, 0);
        lambdaNode.visitEnd();
    }
}
