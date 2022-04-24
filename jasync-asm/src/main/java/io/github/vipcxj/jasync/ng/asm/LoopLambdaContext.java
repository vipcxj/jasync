package io.github.vipcxj.jasync.ng.asm;

import io.github.vipcxj.jasync.ng.utils.Logger;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.BasicValue;

import java.util.*;

import static io.github.vipcxj.jasync.ng.asm.AsmHelper.objectToPrimitive;
import static io.github.vipcxj.jasync.ng.asm.AsmHelper.objectToType;
import static io.github.vipcxj.jasync.ng.asm.Utils.addManyMap;

public class LoopLambdaContext extends AbstractLambdaContext {

    private final AbstractInsnNode[] successors;
    private final LabelNode portalLabel;
    private int portalSlot;

    protected LoopLambdaContext(
            MethodContext methodContext,
            Arguments arguments,
            BranchAnalyzer.Node<? extends BasicValue> node
    ) {
        super(methodContext, MethodContext.MethodType.LOOP_BODY, arguments, node);
        AbstractInsnNode insnNode = node.getInsnNode();
        if (!(insnNode instanceof LabelNode)) {
            // 因为这个指令是至少2个指令的后继，只有 LabelNode 可以是多个指令的后继
            throw new IllegalStateException("This is impossible!");
        }
        LabelNode labelNode = (LabelNode) insnNode;
        this.portalLabel = new LabelNode();
        labelMap.put(labelNode, portalLabel);
        this.successors = methodContext.collectSuccessors(node, (in, n) -> in.clone(labelMap));
    }

    @Override
    protected void addInitCodes() {
        portalSlot = popStack();
    }

    @Override
    protected void addBodyCodes() {
        LinkedList<AbstractInsnNode> insnList = new LinkedList<>();
        List<Integer> insnListMap = new ArrayList<>();
        processSuccessors(successors, insnList, insnListMap);
        addInsnNodes(insnList, insnListMap);

        lambdaNode.instructions.add(portalLabel);
        lambdaMap.add(mappedIndex);
        PackageInsnNode packageInsnNode = new PackageInsnNode();
        methodContext.pushStack(packageInsnNode, lambdaNode, node, successors);
        for (AbstractInsnNode insnNode : packageInsnNode.getInsnNodes()) {
            lambdaNode.instructions.add(insnNode);
            lambdaMap.add(mappedIndex);
        }
        // push portal to stack
        lambdaNode.visitVarInsn(Opcodes.ALOAD, portalSlot);
        lambdaMap.add(mappedIndex);
        AsmHelper.appendStack(lambdaNode, node, 1);
        // push jump lambda
        InvokeDynamicInsnNode jumpLambdaInsnNode = LambdaUtils.invokePortalJump();
        lambdaNode.instructions.add(jumpLambdaInsnNode);
        lambdaMap.add(mappedIndex);
        // thenImmediate(jumpLambda)
        lambdaNode.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                Constants.JPROMISE_NAME,
                Constants.JPROMISE_THEN_IMMEDIATE_NAME,
                Constants.JPROMISE_THEN_IMMEDIATE0_DESC.getDescriptor(),
                true
        );
        lambdaMap.add(mappedIndex);
        lambdaNode.visitInsn(Opcodes.ARETURN);
        lambdaMap.add(mappedIndex);
        Label jumpEnd = new Label();
        lambdaNode.visitLabel(jumpEnd);
        lambdaMap.add(mappedIndex);
    }

    private int popStack() {
        boolean isStatic = methodContext.isStatic();
        MethodContext.MethodType containerType = methodContext.getType();
        // local: this?, JPortal, JStack
        int locals = node.getLocals();
        // The last local var is stack in the loop lambda body. It is not pushed.
        // The last local var is error in the await lambda body. It is not pushed as well.
        int usedLocals = (containerType == MethodContext.MethodType.LOOP_BODY || containerType == MethodContext.MethodType.AWAIT_BODY) ? locals - 1 : locals;
        int portalSlot = isStatic ? 0 : 1;
        int stackSlot = portalSlot + 1;
        if (usedLocals > 0) {
            // load portal to index: 0 / 1 + usedLocals.
            // stack: [] -> JPortal
            lambdaNode.visitVarInsn(Opcodes.ALOAD, portalSlot);
            lambdaMap.add(mappedIndex);
            AsmHelper.updateStack(lambdaNode, 1);
            portalSlot += usedLocals;
            // stack: JPortal -> []
            // locals: ..., -> ..., JPortal
            lambdaNode.visitVarInsn(Opcodes.ASTORE, portalSlot);
            lambdaMap.add(mappedIndex);
            AsmHelper.updateLocal(lambdaNode, portalSlot + 1);

            // load stack to index: 1 / 2 + usedLocals
            // stack: [] -> JStack
            lambdaNode.visitVarInsn(Opcodes.ALOAD, stackSlot);
            lambdaMap.add(mappedIndex);
            stackSlot = portalSlot + 1;
            // stack: JStack -> []
            // locals: ..., JPortal -> ..., JPortal, JStack
            lambdaNode.visitVarInsn(Opcodes.ASTORE, stackSlot);
            lambdaMap.add(mappedIndex);
            AsmHelper.updateLocal(lambdaNode, stackSlot + 1);
        }
        // push stack
        // stack: [] -> JStack
        lambdaNode.visitVarInsn(Opcodes.ALOAD, stackSlot);
        lambdaMap.add(mappedIndex);
        AsmHelper.updateStack(lambdaNode, 1);
        // restore locals
        for (int i = isStatic ? 0 : 1; i < locals;) {
            int nextPos;
            BasicValue value = node.getLocal(i);
            if (value != null && value.getType() != null) {
                Type type = value.getType();
                nextPos = i + type.getSize();
            } else {
                nextPos = i + 1;
            }
            // The last local var in loop lambda body is stack, it is not pushed, so can not be popped.
            // The last local var in await lambda body is error, it is not pushed, so can not be popped as well.
            if (nextPos >= locals && (containerType == MethodContext.MethodType.LOOP_BODY || containerType == MethodContext.MethodType.AWAIT_BODY)) {
                break;
            }
            // stack: JStack -> JStack, JStack
            lambdaNode.visitInsn(Opcodes.DUP);
            lambdaMap.add(mappedIndex);
            // stack: JStack, JStack -> JStack, T
            pop(lambdaNode);
            lambdaMap.add(mappedIndex);
            AsmHelper.updateStack(lambdaNode, 2);
            if (value != null && value.getType() != null) {
                Type type = value.getType();
                int numInsn;
                if (type.getSort() != Type.OBJECT && type.getSort() != Type.ARRAY) {
                    // stack: JStack, T -> JStack, t
                    numInsn = objectToPrimitive(lambdaNode, type);
                } else {
                    numInsn = objectToType(lambdaNode, type);
                }
                addManyMap(lambdaMap, mappedIndex, numInsn);
                // stack: JStack, t -> JStack
                // local: ..., u, ... -> ..., t, ...
                lambdaNode.visitVarInsn(type.getOpcode(Opcodes.ISTORE), i);
            } else {
                // stack: JStack, t -> JStack
                // local: ..., u, ... -> ..., t, ...
                lambdaNode.visitVarInsn(Opcodes.ASTORE, i);
            }
            lambdaMap.add(mappedIndex);
            i = nextPos;
        }
        // restore stacks
        int stackSize = node.getStackSize();
        BasicValue lastValue = null;
        Set<BasicValue> uninitializedValues = new HashSet<>();
        boolean newing = false;
        for (int i = 0; i < stackSize; ++i) {
            BasicValue value = node.getStack(i);
            if (value instanceof JAsyncValue) {
                JAsyncValue asyncValue = (JAsyncValue) value;
                if (asyncValue.isUninitialized()) {
                    if (uninitializedValues.contains(asyncValue) && value == lastValue) {
                        if (newing) {
                            newing = false;
                            // stack: ..., new containerType -> ..., new containerType, new containerType
                            lambdaNode.visitInsn(Opcodes.DUP);
                            lambdaMap.add(mappedIndex);
                            // stack: ..., new containerType, new containerType -> ..., new containerType, new containerType, JStack
                            lambdaNode.visitVarInsn(Opcodes.ALOAD, stackSlot);
                            lambdaMap.add(mappedIndex);
                        } else {
                            throw new IllegalStateException("An uninitialized this object is in the wrong position.");
                        }
                    } else {
                        // stack: ..., JStack -> ...,
                        lambdaNode.visitInsn(Opcodes.POP);
                        lambdaMap.add(mappedIndex);
                        // stack: ..., -> ..., new containerType
                        lambdaNode.visitTypeInsn(Opcodes.NEW, asyncValue.getType().getInternalName());
                        lambdaMap.add(mappedIndex);
                        newing = true;
                    }
                    uninitializedValues.add(asyncValue);
                    lastValue = value;
                    continue;
                }
            }
            if (newing) {
                newing = false;
                // stack: ..., new containerType -> ..., new containerType, JStack
                lambdaNode.visitVarInsn(Opcodes.ALOAD, stackSlot);
                lambdaMap.add(mappedIndex);
                Logger.warn("An uninitialized this object lost.");
            }
            // stack: ..., JStack -> ..., T
            pop(lambdaNode);
            lambdaMap.add(mappedIndex);
            if (value != null && value.getType() != null) {
                Type type = value.getType();
                if (type.getSort() != Type.OBJECT && type.getSort() != Type.ARRAY) {
                    // stack: ..., T -> ..., t
                    int numInsn = objectToPrimitive(lambdaNode, type);
                    addManyMap(lambdaMap, mappedIndex, numInsn);
                }
            }
            // stack: ..., t -> ..., t, JStack
            lambdaNode.visitVarInsn(Opcodes.ALOAD, stackSlot);
            lambdaMap.add(mappedIndex);
            lastValue = value;
        }
        if (newing) {
            // stack: ..., new containerType -> ..., new containerType, JStack
            lambdaNode.visitVarInsn(Opcodes.ALOAD, stackSlot);
            lambdaMap.add(mappedIndex);
            Logger.warn("An uninitialized this object lost.");
        }
        // stack: ..., JStack -> ...
        lambdaNode.visitInsn(Opcodes.POP);
        lambdaMap.add(mappedIndex);
        AsmHelper.updateStack(lambdaNode, 1 + stackSize);
        return portalSlot;
    }

    private void pop(MethodNode lambdaNode) {
        lambdaNode.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                Constants.JSTACK_NAME,
                Constants.JSTACK_POP_NAME,
                Constants.JSTACK_POP_DESC.getDescriptor(),
                true
        );
    }
}
