package io.github.vipcxj.jasync.ng.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class AwaitLambdaContext extends AbstractLambdaContext {

    private final int locals;
    private final AbstractInsnNode[] successors;

    protected AwaitLambdaContext(
            MethodContext methodContext,
            Arguments arguments,
            BranchAnalyzer.Node<? extends BasicValue> node,
            int locals
    ) {
        super(methodContext, MethodContext.MethodType.AWAIT_BODY, arguments, node);
        this.locals = locals;
        this.successors = methodContext.collectSuccessors(node, (in, n) -> in.clone(labelMap));
    }

    @Override
    protected int validLocals() {
        // local: this?, x, y, z, await value, error
        // locals => this?, x, y, z
        // base valid locals => locals + await value
        return locals;
    }

    @Override
    protected void addInitCodes() {
        updateTcbAndLocal(startLabelNode, node);
        boolean isStatic = methodContext.isStatic();
        // arguments: x, y, z, a, b, await result, await error, context
        int errorOffset = arguments.argumentLocalOffset(isStatic, -2);
        Label errorRethrowEndLabel = new Label();
        // load error to stack
        lambdaNode.visitVarInsn(Opcodes.ALOAD, errorOffset);
        lambdaMap.add(mappedIndex);
        // consume error from stack and jump if null
        lambdaNode.visitJumpInsn(Opcodes.IFNULL, errorRethrowEndLabel);
        lambdaMap.add(mappedIndex);
        // if error non-null, so step here, load error to stack again
        lambdaNode.visitVarInsn(Opcodes.ALOAD, errorOffset);
        lambdaMap.add(mappedIndex);
        // rethrow the error
        lambdaNode.visitInsn(Opcodes.ATHROW);
        lambdaMap.add(mappedIndex);
        // insert the label which jump to if error is null
        lambdaNode.visitLabel(errorRethrowEndLabel);
        lambdaMap.add(mappedIndex);

        // arguments: x, y, z, a, b, await result, await error, context -> stack: a, b, await result
        // locals: this?, x, y, z, a, b, await type, error type, context
        int j = isStatic ? 0 : 1;
        for (Arguments.ExtendType extendType : arguments.getTypes()) {
            // arguments 的构成恰好同构于 locals + stack，
            // 虽然最后一位, arguments 中是 await 的结果，而 frame 中是 Promise. 但它们都是 Object, 可以做相同处理.
            if (j >= locals) {
                // error not push to stack.
                if (j == errorOffset) {
                    break;
                }
                if (extendType.isInitialized()) {
                    lambdaNode.visitVarInsn(extendType.getOpcode(Opcodes.ILOAD), j);
                    lambdaMap.add(mappedIndex);
                    j += extendType.getSize();
                } else {
                    if (extendType.getOffset() == 0) {
                        lambdaNode.visitTypeInsn(Opcodes.NEW, extendType.getType().getInternalName());
                        lambdaMap.add(mappedIndex);
                    } else if (extendType.getOffset() == 1) {
                        lambdaNode.visitInsn(Opcodes.DUP);
                        lambdaMap.add(mappedIndex);
                    } else {
                        throw new IllegalStateException("The uninitialized this object is in a wrong position.");
                    }
                }
            } else {
                j += extendType.getSize();
            }
        }
        AsmHelper.updateStack(lambdaNode, node.getStackSize());
    }

    @Override
    protected void addBodyCodes() {
        LinkedList<AbstractInsnNode> insnList = new LinkedList<>();
        List<Integer> insnListMap = new ArrayList<>();
        processSuccessors(successors, insnList, insnListMap);
        addInsnNodes(insnList, insnListMap);
    }
}
