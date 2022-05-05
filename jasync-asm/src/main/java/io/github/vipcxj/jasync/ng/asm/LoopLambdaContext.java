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
    private final int validLocals;

    protected LoopLambdaContext(
            MethodContext methodContext,
            Arguments arguments,
            BranchAnalyzer.Node<? extends BasicValue> node
    ) {
        super(methodContext, MethodContext.MethodType.LOOP_BODY, arguments, node);
        this.validLocals = methodContext.calcValidLocals(node);
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
    protected int validLocals() {
        return validLocals;
    }

    @Override
    protected void addInitCodes() {
        restoreLocalAndStack();
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
        // stack: ... -> ..., jumpIndex
        packageInsnNode.getInsnNodes().add(AsmHelper.loadConstantInt(mappedIndex));
        // stack: ..., jumpIndex -> ..., jumpIndex, Object[]
        methodContext.collectLocalsAndStackToArrayArg(packageInsnNode, lambdaNode, node, successors, validLocals);
        // JPromise.jump(jumpIndex, localVars)
        // stack: ..., jumpIndex, Object[] -> ..., JPromise
        packageInsnNode.getInsnNodes().add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                Constants.JPROMISE_NAME,
                Constants.JPROMISE_JUMP_NAME,
                Constants.JPROMISE_JUMP_DESC.getDescriptor(), true
        ));
        // return
        // stack: ..., JPromise -> ...
        packageInsnNode.getInsnNodes().add(new InsnNode(Opcodes.ARETURN));
        packageInsnNode.complete();
        for (AbstractInsnNode insnNode : packageInsnNode.getInsnNodes()) {
            lambdaNode.instructions.add(insnNode);
            lambdaMap.add(mappedIndex);
        }
    }

    private void restoreLocalAndStack() {
        boolean isStatic = methodContext.isStatic();
        int offset = isStatic ? 0 : 1;
        // locals: this?, localVars
        // push localVars
        // stack: [] -> localVars
        lambdaNode.visitVarInsn(Opcodes.ALOAD, offset);
        lambdaMap.add(mappedIndex);
        AsmHelper.updateStack(lambdaNode, 1);
        // restore locals
        int j = 0;
        for (int i = offset; i < validLocals;) {
            BasicValue value = node.getLocal(i);
            // stack: localVars -> localVars, localVars
            lambdaNode.visitInsn(Opcodes.DUP);
            lambdaMap.add(mappedIndex);
            // stack: localVars, localVars -> localVars, localVars, int
            AsmHelper.visitConstantInt(lambdaNode, j++);
            lambdaMap.add(mappedIndex);
            AsmHelper.updateStack(lambdaNode, 3);
            // stack: localVars, localVars, int -> localVars, Object
            lambdaNode.visitInsn(Opcodes.AALOAD);
            lambdaMap.add(mappedIndex);
            if (value != null && value.getType() != null) {
                Type type = value.getType();
                int numInsn;
                if (type.getSort() != Type.OBJECT && type.getSort() != Type.ARRAY) {
                    // stack: localVars, Object -> localVars, primitive
                    numInsn = objectToPrimitive(lambdaNode, type);
                } else {
                    numInsn = objectToType(lambdaNode, type);
                }
                addManyMap(lambdaMap, mappedIndex, numInsn);
                // stack: localVars, t -> localVars
                // local: ..., u, ... -> ..., t, ...
                lambdaNode.visitVarInsn(type.getOpcode(Opcodes.ISTORE), i);
                i += type.getSize();
            } else {
                // stack: localVars, t -> localVars
                // local: ..., u, ... -> ..., t, ...
                lambdaNode.visitVarInsn(Opcodes.ASTORE, i);
                ++i;
            }
            lambdaMap.add(mappedIndex);
        }
        // restore stacks
        // stack: localVars
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
                            // stack: ..., new containerType, new containerType -> ..., new containerType, new containerType, localVars
                            lambdaNode.visitVarInsn(Opcodes.ALOAD, validLocals);
                            lambdaMap.add(mappedIndex);
                        } else {
                            throw new IllegalStateException("An uninitialized this object is in the wrong position.");
                        }
                    } else {
                        // stack: ..., localVars -> ...,
                        // locals: ..., -> ..., localVars
                        lambdaNode.visitVarInsn(Opcodes.ASTORE, validLocals);
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
                // stack: ..., new containerType -> ..., new containerType, localVars
                lambdaNode.visitVarInsn(Opcodes.ALOAD, validLocals);
                lambdaMap.add(mappedIndex);
                Logger.warn("An uninitialized this object lost.");
            }
            // stack: ..., localVars -> ..., localVars, int
            AsmHelper.visitConstantInt(lambdaNode, validLocals + i);
            lambdaMap.add(mappedIndex);
            // stack: ..., localVars, int -> ..., T
            lambdaNode.visitInsn(Opcodes.AALOAD);
            lambdaMap.add(mappedIndex);
            if (value != null && value.getType() != null) {
                Type type = value.getType();
                if (type.getSort() != Type.OBJECT && type.getSort() != Type.ARRAY) {
                    // stack: ..., T -> ..., t
                    int numInsn = objectToPrimitive(lambdaNode, type);
                    addManyMap(lambdaMap, mappedIndex, numInsn);
                }
            }
            // stack: ..., t -> ..., t, localVars
            lambdaNode.visitVarInsn(Opcodes.ALOAD, validLocals);
            lambdaMap.add(mappedIndex);
            lastValue = value;
        }
        if (newing) {
            // stack: ..., new containerType -> ..., new containerType, localVars
            lambdaNode.visitVarInsn(Opcodes.ALOAD, validLocals);
            lambdaMap.add(mappedIndex);
            Logger.warn("An uninitialized this object lost.");
        }
        // stack: ..., localVars -> ...
        lambdaNode.visitInsn(Opcodes.POP);
        lambdaMap.add(mappedIndex);
        AsmHelper.updateStack(lambdaNode, 1 + stackSize);
    }
}
