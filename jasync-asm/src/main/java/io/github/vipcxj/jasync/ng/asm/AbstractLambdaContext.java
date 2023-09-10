package io.github.vipcxj.jasync.ng.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.BasicValue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractLambdaContext {
    protected final MethodContext methodContext;
    protected final MethodNode lambdaNode;
    protected final MethodContext.MethodType methodType;
    protected final Arguments arguments;
    protected final BranchAnalyzer.Node<? extends BasicValue> node;
    protected final LabelMap labelMap;
    protected final List<Integer> lambdaMap;
    protected final int index;
    protected final int mappedIndex;
    protected final List<LocalVariableNode> completedLocalVariables;
    protected final LocalVariableNode[] processingLocalVariables;
    protected final List<TryCatchBlockNode> processingTcbNode;
    protected final List<TryCatchBlockNode> completedTcbNode;
    protected LabelNode startLabelNode;
    protected LabelNode completeInitLabelNode;

    protected AbstractLambdaContext(
            MethodContext methodContext,
            MethodContext.MethodType methodType,
            Arguments arguments,
            BranchAnalyzer.Node<? extends BasicValue> node
    ) {
        this.methodContext = methodContext;
        this.lambdaNode = methodContext.createLambdaNode(arguments);
        AsmHelper.updateStack(lambdaNode, methodContext.getMv().maxStack);
        AsmHelper.updateLocal(lambdaNode, methodContext.getMv().maxLocals);
        this.methodType = methodType;
        this.arguments = arguments;
        this.node = node;
        this.labelMap = new LabelMap();
        this.lambdaMap = new ArrayList<>();
        this.index = node.getIndex();
        this.mappedIndex = methodContext.mapped(index);

        this.completedLocalVariables = new ArrayList<>();
        this.processingLocalVariables = arguments.toLocalVariables();
        this.processingTcbNode = new ArrayList<>();
        this.completedTcbNode = new ArrayList<>();
    }

    public MethodNode getLambdaNode() {
        return lambdaNode;
    }

    public void buildLambda() {
        lambdaNode.visitCode();
        addStartLabelNode();
        addInitCodes();
        addCompleteInitLabelNode();
        addBodyCodes();
        addEndLabelNode();
        lambdaNode.tryCatchBlocks = completedTcbNode;
        lambdaNode.localVariables = completedLocalVariables;
        methodContext.addLambdaContext(lambdaNode, lambdaMap, methodType, validLocals());
    }

    protected abstract int validLocals();
    protected abstract void addInitCodes();
    protected abstract void addBodyCodes();

    private void addStartLabelNode() {
        startLabelNode = new LabelNode();
        lambdaNode.instructions.add(startLabelNode);
        lambdaMap.add(mappedIndex);
        for (LocalVariableNode localVariableNode : this.processingLocalVariables) {
            // localVariableNode here is created from arguments, so they should start here.
            if (localVariableNode != null) {
                localVariableNode.start = startLabelNode;
            }
        }
        methodContext.updateLocalVar(processingLocalVariables, completedLocalVariables, startLabelNode, node);
    }

    private void addCompleteInitLabelNode() {
        if (lambdaNode.instructions.size() > 1) {
            completeInitLabelNode = new LabelNode();
            lambdaNode.instructions.add(completeInitLabelNode);
            lambdaMap.add(mappedIndex);
        }
    }

    private void addEndLabelNode() {
        LabelNode endLabel;
        AbstractInsnNode lastNode = lambdaNode.instructions.getLast();
        if (lastNode instanceof LabelNode) {
            endLabel = (LabelNode) lastNode;
            methodContext.completeLocalVar(processingLocalVariables, completedLocalVariables, null, false);
        } else {
            endLabel = new LabelNode();
            lambdaNode.instructions.add(endLabel);
            lambdaMap.add(mappedIndex);
            methodContext.completeLocalVar(processingLocalVariables, completedLocalVariables, endLabel, false);
        }
        addThisVarToLocalVarNodes(endLabel);
        methodContext.completeTryCatchBlockNodes(processingTcbNode, completedTcbNode, endLabel);
    }

    private LocalVariableNode findThisVar() {
        if (methodContext.isStatic())
            return null;
        for (LocalVariableNode localVariableNode : processingLocalVariables) {
            if (localVariableNode != null && localVariableNode.index == 0) {
                return localVariableNode;
            }
        }
        return null;
    }

    private void addThisVarToLocalVarNodes(LabelNode endLabel) {
        LocalVariableNode thisVarNode = findThisVar();
        if (thisVarNode != null && thisVarNode.start != null) {
            thisVarNode.end = endLabel;
            completedLocalVariables.add(thisVarNode);
        }
    }

    protected void addInsnNodes(List<AbstractInsnNode> insnList, List<Integer> insnListMap) {
        Iterator<AbstractInsnNode> insnIter = insnList.iterator();
        Iterator<Integer> insnMapIter = insnListMap.iterator();
        while (insnIter.hasNext()) {
            lambdaNode.instructions.add(insnIter.next());
            lambdaMap.add(insnMapIter.next());
        }
    }

    protected void processSuccessors(
            AbstractInsnNode[] successors,
            LinkedList<AbstractInsnNode> insnList, List<Integer> insnListMap
    ) {
        List<AbstractInsnNode> preInsnList = new LinkedList<>();
        List<Integer> preInsnListMap = new ArrayList<>();
        List<BranchAnalyzer.Node<? extends BasicValue>> preFrames = new LinkedList<>();
        int i = 0;
        boolean reconnect = false;
        for (AbstractInsnNode insnNode : successors) {
            if (insnNode != null) {
                // exclude node itself.
                if (i != index) {
                    List<AbstractInsnNode> target;
                    List<Integer> targetMap;
                    if (i < index) {
                        target = preInsnList;
                        targetMap = preInsnListMap;
                        if (i == index - 1) {
                            BranchAnalyzer.Node<BasicValue> frame = methodContext.getFrames()[i];
                            if (frame.getSuccessors().contains(node)) {
                                reconnect = true;
                            }
                        }
                    } else {
                        target = insnList;
                        targetMap = insnListMap;
                    }
                    BranchAnalyzer.Node<BasicValue> frame = methodContext.getFrames()[i];
                    int originalIndex = methodContext.mapped(i);
                    if (insnNode instanceof PackageInsnNode) {
                        PackageInsnNode packageInsnNode = (PackageInsnNode) insnNode;
                        for (AbstractInsnNode n : packageInsnNode.getInsnNodes()) {
                            processLambdaNode(
                                    insnList, preFrames,
                                    target, targetMap,
                                    frame, originalIndex, n
                            );
                        }
                    } else {
                        processLambdaNode(
                                insnList, preFrames,
                                target, targetMap,
                                frame, originalIndex, insnNode
                        );
                    }
                }
            }
            ++i;
        }
        LabelNode reconnectLabel = new LabelNode();
        if (reconnect) {
            insnList.add(0, reconnectLabel);
            insnListMap.add(mappedIndex);
        }
        Iterator<AbstractInsnNode> preInsnIter = preInsnList.iterator();
        Iterator<Integer> preInsnMapIter = preInsnListMap.iterator();
        Iterator<BranchAnalyzer.Node<? extends BasicValue>> preFrameIter = preFrames.iterator();
        while (preInsnIter.hasNext()) {
            AbstractInsnNode preInsn = preInsnIter.next();
            Integer preInsnMap = preInsnMapIter.next();
            BranchAnalyzer.Node<? extends BasicValue> preFrame = preFrameIter.next();
            processLambdaNode(
                    insnList, preFrames,
                    insnList, insnListMap,
                    preFrame, preInsnMap, preInsn
            );
        }
        if (reconnect) {
            insnList.add(new JumpInsnNode(Opcodes.GOTO, reconnectLabel));
            insnListMap.add(mappedIndex);
        }
    }

    protected LabelNode updateTcbAndLocal(
            AbstractInsnNode n,
            BranchAnalyzer.Node<? extends BasicValue> frame
    ) {
        LabelNode tcStart = methodContext.updateTryCatchBlockNodes(processingTcbNode, completedTcbNode, n, frame, labelMap);
        methodContext.updateLocalVar(processingLocalVariables, completedLocalVariables, n, frame);
        return tcStart;
    }

    private void processLambdaNode(
            LinkedList<AbstractInsnNode> insnList,
            List<BranchAnalyzer.Node<? extends BasicValue>> preFrames,
            List<AbstractInsnNode> target,
            List<Integer> targetMap,
            BranchAnalyzer.Node<? extends BasicValue> frame,
            int originalIndex,
            AbstractInsnNode n
    ) {
        if (target == insnList) {
            LabelNode tcStart = updateTcbAndLocal(n, frame);
            if (tcStart != null) {
                target.add(tcStart);
                targetMap.add(originalIndex);
            }
        } else {
            preFrames.add(frame);
        }
        target.add(n);
        targetMap.add(originalIndex);
    }
}
