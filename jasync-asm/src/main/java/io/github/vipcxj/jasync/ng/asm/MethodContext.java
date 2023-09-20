package io.github.vipcxj.jasync.ng.asm;

import io.github.vipcxj.jasync.ng.utils.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static io.github.vipcxj.jasync.ng.asm.AsmHelper.primitiveToObject;

public class MethodContext {
    private final ClassContext classContext;
    private final MethodNode mv;
    private final JAsyncInfo info;
    private final BranchAnalyzer.Node<BasicValue>[] frames;
    private final List<Integer> localsToUpdate;
    private final List<Integer> stacksToUpdate;
    private final MethodType type;
    private final int head;
    private List<Integer> map;

    protected final MethodContext parent;
    private final String rootMethodName;
    private final int baseValidLocals;
    private final List<MethodContext> children;
    private final AbstractInsnNode[] insnNodes;
    private List<Set<Integer>> loops;
    private int contextVarIndex;
    private int localVarBase;

    public MethodContext(ClassContext classContext, MethodNode mv, String rootMethodName, JAsyncInfo info) {
        // The root baseValidLocals = AsmHelper.calcMethodArgLocals(mv) - 1
        // Because the last argument is JContext which should be ignored.
        this(classContext, mv, info, null, MethodType.TOP, null, rootMethodName, AsmHelper.calcMethodArgLocals(mv) - 1);
    }

    public MethodContext(
            ClassContext classContext,
            MethodNode mv,
            JAsyncInfo info,
            List<Integer> map,
            MethodType type,
            MethodContext parent,
            String rootMethodName,
            int baseValidLocals
    ) {
        this.classContext = classContext;
        this.mv = mv;
        this.info = info != null ? info : (parent != null ? parent.getInfo() : null);
        this.contextVarIndex = AsmHelper.calcMethodArgLocals(mv) - 1;
        this.map = map;
        this.localsToUpdate = new ArrayList<>();
        this.stacksToUpdate = new ArrayList<>();
        this.localVarBase = 0;
        this.type = type;
        this.head = mapped(0);
        BranchAnalyzer analyzer = new BranchAnalyzer(true);
        try {
            analyzer.analyzeAndComputeMaxs(classContext.getInternalName(), mv);
            this.frames = analyzer.getNodes();
        } catch (AnalyzerException e) {
            AsmHelper.printFrameProblem(
                    classContext.getInternalName(),
                    mv,
                    analyzer.getNodes(),
                    map,
                    e,
                    JAsyncInfo.BYTE_CODE_OPTION_FULL_SUPPORT,
                    -5,
                    4
            );
            throw new RuntimeException(e);
        }
        this.parent = parent;
        this.rootMethodName = rootMethodName;
        this.baseValidLocals = baseValidLocals;
        this.children = new ArrayList<>();
        this.insnNodes = new AbstractInsnNode[this.frames.length];
    }

    public MethodContext createChild(MethodNode methodNode, List<Integer> map, MethodType type, int parentLocals) {
        return new MethodContext(classContext, methodNode, null, map, type, this, rootMethodName, parentLocals);
    }

    public ClassContext getClassContext() {
        return classContext;
    }

    public MethodNode getMv() {
        return mv;
    }

    public List<Integer> getMap() {
        return map;
    }

    public MethodType getType() {
        return type;
    }

    public int getHead() {
        return head;
    }

    public JAsyncInfo getInfo() {
        return info;
    }

    public BranchAnalyzer.Node<BasicValue>[] getFrames() {
        return frames;
    }

    public boolean isStatic() {
        return AsmHelper.isStatic(mv);
    }

    private MethodContext getRootMethodContext() {
        return parent != null ? parent.getRootMethodContext() : this;
    }

    public Type classType() {
        return Type.getObjectType(classContext.getInternalName());
    }

    public void updateLocals(int locals) {
        localsToUpdate.add(locals);
    }

    public void updateStacks(int stacks) {
        stacksToUpdate.add(stacks);
    }

    public void updateMax() {
        mv.maxLocals = Math.max(mv.maxLocals, localsToUpdate.stream().mapToInt(i -> i).max().orElse(0));
        mv.maxStack = Math.max(mv.maxStack, stacksToUpdate.stream().mapToInt(i -> i).max().orElse(0));
    }

    public void addLambdaContext(MethodNode lambdaNode, List<Integer> map, MethodType type, int parentLocals) {
        MethodContext childContext = createChild(lambdaNode, map, type, parentLocals);
        children.add(childContext);
        this.classContext.addLambda(getRootMethodContext(), childContext);
    }

    private void clearInsnList(InsnList insnList) {
        ListIterator<AbstractInsnNode> iterator = insnList.iterator();
        while (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    public void process() {
        BranchAnalyzer.Node<BasicValue>[] nodes = getFrames();
        if (nodes.length > 0) {
            loops = GraphUtils.tarjan(nodes);
            // 新指令全部置空
            Arrays.fill(insnNodes, null);
            // 填充新指令，多半填不满，但没关系，后面会处理
            process(nodes[0]);
            // 清空已有的指令
            clearInsnList(getMv().instructions);
            List<LocalVariableNode> completedLocalVariableNodes = new ArrayList<>();
            LocalVariableNode[] processingLocalVariableNodes = new LocalVariableNode[mv.maxLocals];
            List<TryCatchBlockNode> processingTcbNodes = new ArrayList<>();
            List<TryCatchBlockNode> completedTcbNodes = new ArrayList<>();
            InsnList newInsnList = new InsnList();
            List<Integer> newMap = new ArrayList<>();
            // 将处理过的新指令放回方法中
            for (int i = 0; i < insnNodes.length; ++i) {
                AbstractInsnNode insnNode = insnNodes[i];
                // 呼应前文，新指令填不满，大概率有空，空的直接丢弃
                if (insnNode != null) {
                    int mappedIndex = mapped(i);
                    BranchAnalyzer.Node<BasicValue> frame = frames[i];
                    AbstractInsnNode firstNode = getFirstNode(insnNode);
                    if (firstNode != null) {
                        LabelNode newLabelNode = updateTryCatchBlockNodes(processingTcbNodes, completedTcbNodes, firstNode, frame, null);
                        if (newLabelNode != null) {
                            newMap.add(mappedIndex);
                            newInsnList.add(newLabelNode);
                        }
                    }
                    Type needCastTo = frame.getNeedCastTo();
                    if (needCastTo != null) {
                        newMap.add(mappedIndex);
                        newInsnList.add(new TypeInsnNode(Opcodes.CHECKCAST, needCastTo.getInternalName()));
                    }
                    if (insnNode instanceof PackageInsnNode) {
                        PackageInsnNode packageInsnNode = (PackageInsnNode) insnNode;
                        for (AbstractInsnNode node : packageInsnNode.getInsnNodes()) {
                            newMap.add(mappedIndex);
                            newInsnList.add(node);
                        }
                        for (int j = 0; j < processingLocalVariableNodes.length; ++j) {
                            LocalVariableNode variableNode = processingLocalVariableNodes[j];
                            if (variableNode != null && variableNode.start != null) {
                                variableNode.end = packageInsnNode.getEndNode();
                            }
                            pushLocalVariable(processingLocalVariableNodes, j, completedLocalVariableNodes, null);
                        }
                    } else {
                        newMap.add(mappedIndex);
                        newInsnList.add(insnNode);
                        updateLocalVar(processingLocalVariableNodes, completedLocalVariableNodes, insnNode, frame, -1);
                    }
                }
            }
            LabelNode endNode;
            if (newInsnList.getLast() instanceof LabelNode) {
                endNode = (LabelNode) newInsnList.getLast();
                completeLocalVar(processingLocalVariableNodes, completedLocalVariableNodes, null, true);
            } else {
                endNode = new LabelNode();
                newInsnList.add(endNode);
                completeLocalVar(processingLocalVariableNodes, completedLocalVariableNodes, endNode, true);
            }
            completeTryCatchBlockNodes(processingTcbNodes, completedTcbNodes, endNode);
            filterTryCatchBlockNodes(completedTcbNodes, newInsnList);
            map = newMap;
            getMv().instructions = newInsnList;
            getMv().tryCatchBlocks = completedTcbNodes;
            getMv().localVariables = completedLocalVariableNodes;
            updateMax();
            for (MethodContext child : children) {
                child.process();
            }
            restoreContextVar();
            recordLineNumbers();
        }
    }

    private void restoreContextVar() {
        int newContextVarIndex = AsmHelper.calcFreeVarIndex(mv, contextVarIndex, 1);
        if (newContextVarIndex != contextVarIndex) {
            mv.instructions.insertBefore(mv.instructions.getFirst(), new VarInsnNode(Opcodes.ASTORE, newContextVarIndex));
            mv.instructions.insertBefore(mv.instructions.getFirst(), new VarInsnNode(Opcodes.ALOAD, contextVarIndex));
            AsmHelper.updateLocal(mv, newContextVarIndex);
            AsmHelper.updateStack(mv, 1);
            this.contextVarIndex = newContextVarIndex;
        }
    }

    private void recordLineNumbers() {
        LineNumberNode lastLineNumberNode = null;
        AbstractInsnNode lastPositionNode = null;
        for (AbstractInsnNode insnNode : mv.instructions) {
            if (insnNode instanceof LineNumberNode) {
                lastLineNumberNode = (LineNumberNode) insnNode;
                lastPositionNode = lastLineNumberNode;
            } else if (insnNode.getOpcode() == Opcodes.ARETURN && lastLineNumberNode != null) {
                AbstractInsnNode node = new VarInsnNode(Opcodes.ALOAD, contextVarIndex);
                mv.instructions.insert(lastPositionNode, node);
                lastPositionNode = node;
                node = AsmHelper.loadConstantInt(lastLineNumberNode.line);
                mv.instructions.insert(lastPositionNode, node);
                lastPositionNode = node;
                node = new MethodInsnNode(
                        Opcodes.INVOKEINTERFACE,
                        Constants.JCONTEXT_NAME,
                        Constants.JCONTEXT_SET_LINE_NUMBER_NAME,
                        Constants.JCONTEXT_SET_LINE_NUMBER_DESC.getDescriptor(),
                        true
                );
                mv.instructions.insert(lastPositionNode, node);
                lastPositionNode = node;
                node = new InsnNode(Opcodes.POP);
                mv.instructions.insert(lastPositionNode, node);
            } else if (insnNode instanceof FrameNode){
                lastPositionNode = insnNode;
            }
        }
    }

    private static boolean equals(String a, String b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }

    private static boolean equals(LocalVar localVar, LocalVariableNode localVariableNode) {
        if (localVar == null && localVariableNode == null) {
            return true;
        }
        if (localVar == null || localVariableNode == null) {
            return false;
        }
        return equals(localVar.getName(), localVariableNode.name)
                && equals(localVar.getDesc(), localVariableNode.desc)
                // the localVariableNode created from Arguments has a null signature, so don't check it.
                && (localVariableNode.signature == null || equals(localVar.getSignature(), localVariableNode.signature));
    }

    private void pushLocalVariable(LocalVariableNode[] processingLocalVariables, int i, List<LocalVariableNode> completedLocalVariables, LabelNode endNode) {
        LocalVariableNode localVariableNode = processingLocalVariables[i];
        if (localVariableNode != null && localVariableNode.start != null && (localVariableNode.end != null || endNode != null)) {
            if (localVariableNode.end == null) {
                localVariableNode.end = endNode;
            }
            completedLocalVariables.add(localVariableNode);
        }
        processingLocalVariables[i] = null;
    }

    void updateLocalVar(
            LocalVariableNode[] processingLocalVariables,
            List<LocalVariableNode> completedLocalVariables,
            AbstractInsnNode insnNode, BranchAnalyzer.Node<? extends BasicValue> frame, int validLocals
    ) {
        if (validLocals < 0) {
            validLocals = calcValidLocals(frame);
        }
        updateLocalVar(processingLocalVariables, completedLocalVariables, insnNode, frame.getLocalVars(), validLocals);
    }

    void updateLocalVar(
            LocalVariableNode[] processingLocalVariables,
            List<LocalVariableNode> completedLocalVariables,
            AbstractInsnNode insnNode, LocalVar[] localVars, int validLocals
    ) {
        int size = Math.min(processingLocalVariables.length, validLocals);
        for (int i = 0; i < size; ++i) {
            LocalVar localVar = localVars[i];
            LocalVariableNode processingLocalVariable = processingLocalVariables[i];
            if (!equals(localVar, processingLocalVariable)) {
                if (localVar == null) {
                    pushLocalVariable(processingLocalVariables, i, completedLocalVariables, insnNode instanceof LabelNode ? (LabelNode) insnNode : null);
                } else if (processingLocalVariable == null) {
                    LocalVariableNode variableNode = new LocalVariableNode(localVar.getName(), localVar.getDesc(), localVar.getSignature(), null, null, i);
                    if (insnNode instanceof LabelNode) {
                        variableNode.start = (LabelNode) insnNode;
                    }
                    processingLocalVariables[i] = variableNode;
                } else {
                    pushLocalVariable(processingLocalVariables, i, completedLocalVariables, insnNode instanceof LabelNode ? (LabelNode) insnNode : null);
                    LocalVariableNode variableNode = new LocalVariableNode(localVar.getName(), localVar.getDesc(), localVar.getSignature(), null, null, i);
                    if (insnNode instanceof LabelNode) {
                        variableNode.start = (LabelNode) insnNode;
                    }
                    processingLocalVariables[i] = variableNode;
                }
            } else if (processingLocalVariable != null) {
                // the localVariableNode created from Arguments has a null signature, so fix it here.
                if (processingLocalVariable.signature == null && localVar.getSignature() != null) {
                    processingLocalVariable.signature = localVar.getSignature();
                }
                if (insnNode instanceof LabelNode) {
                    if (processingLocalVariable.start == null) {
                        processingLocalVariable.start = (LabelNode) insnNode;
                    } else {
                        processingLocalVariable.end = (LabelNode) insnNode;
                    }
                }
            }
        }
    }

    void completeLocalVar(LocalVariableNode[] processingLocalVariables, List<LocalVariableNode> completedLocalVariables, LabelNode endNode, boolean includeThis) {
        for (int i = 0; i < processingLocalVariables.length; ++i) {
            LocalVariableNode localVariableNode = processingLocalVariables[i];
            if (localVariableNode != null && (includeThis || isStatic() || localVariableNode.index != 0)) {
                pushLocalVariable(processingLocalVariables, i, completedLocalVariables, endNode);
            }
        }
    }

    /**
     * 更新 TryCatchBlock节点信息
     * @param processings 正在处理中的 tcb节点
     * @param completes 已经处理完成的 tcb节点
     * @param insnNode 当前节点
     * @param frame 当前帧
     * @param labelMap 标签映射，用于 copy 指令，可以为空
     * @return 可能需要插入的新标签指令，可能为 null
     */
    LabelNode updateTryCatchBlockNodes(
            List<TryCatchBlockNode> processings,
            List<TryCatchBlockNode> completes,
            AbstractInsnNode insnNode, BranchAnalyzer.Node<? extends BasicValue> frame,
            LabelMap labelMap
    ) {
        List<TryCatchBlockNode> handlers = frame.getHandlers();
        LabelNode newLabelNode = null;
        // old handler to new handler
        Map<TryCatchBlockNode, TryCatchBlockNode> map = new HashMap<>();
        // new handler to old handler
        Map<TryCatchBlockNode, TryCatchBlockNode> reverseMap;
        if (!handlers.isEmpty()) {
            reverseMap = new HashMap<>();
            for (TryCatchBlockNode handler : handlers) {
                LabelNode newHandler = labelMap != null ? labelMap.get(handler.handler) : handler.handler;
                for (TryCatchBlockNode tryCatchBlockNode : processings) {
                    if (tryCatchBlockNode.handler == newHandler && Objects.equals(tryCatchBlockNode.type, handler.type)) {
                        map.put(handler, tryCatchBlockNode);
                        reverseMap.put(tryCatchBlockNode, handler);
                        break;
                    }
                }
            }
            // create new node
            for (TryCatchBlockNode handler : handlers) {
                if (!map.containsKey(handler)) {
                    TryCatchBlockNode newTryCatchBlockNode = new TryCatchBlockNode(
                            null, null,
                            labelMap != null ? labelMap.get(handler.handler) : handler.handler,
                            handler.type
                    );
                    if (insnNode instanceof LabelNode) {
                        newTryCatchBlockNode.start = (LabelNode) insnNode;
                    } else {
                        if (newLabelNode == null) {
                            newLabelNode = new LabelNode();
                        }
                        newTryCatchBlockNode.start = newLabelNode;
                    }
                    processings.add(newTryCatchBlockNode);
                    map.put(handler, newTryCatchBlockNode);
                    reverseMap.put(newTryCatchBlockNode, handler);
                }
            }
        } else {
            reverseMap = Collections.emptyMap();
        }
        // end node
        ListIterator<TryCatchBlockNode> iterator = processings.listIterator();
        while (iterator.hasNext()) {
            TryCatchBlockNode tryCatchBlockNode = iterator.next();
            if (!reverseMap.containsKey(tryCatchBlockNode)) {
                if (insnNode instanceof LabelNode) {
                    tryCatchBlockNode.end = (LabelNode) insnNode;
                } else {
                    if (newLabelNode == null) {
                        newLabelNode = new LabelNode();
                    }
                    tryCatchBlockNode.end = newLabelNode;
                }
                completes.add(tryCatchBlockNode);
                iterator.remove();
            }
        }
        return newLabelNode;
    }

    @SuppressWarnings("UnusedReturnValue")
    LabelNode completeTryCatchBlockNodes(List<TryCatchBlockNode> processing, List<TryCatchBlockNode> completes, LabelNode endNode) {
        if (processing.isEmpty()) {
            return null;
        }
        LabelNode newEndNode = null;
        if (endNode == null) {
            endNode = newEndNode = new LabelNode();
        }
        for (TryCatchBlockNode tryCatchBlockNode : processing) {
            tryCatchBlockNode.end = endNode;
            completes.add(tryCatchBlockNode);
        }
        processing.clear();
        return newEndNode;
    }

    void filterTryCatchBlockNodes(List<TryCatchBlockNode> completedTcbNodes, InsnList insnList) {
        completedTcbNodes.removeIf(tcb -> !insnList.contains(tcb.handler));
    }

    /**
     * 获取头指令，因为传入的指令可能是 PackageInsnNode 类型，这种指令是一系列指令的集合，取其第一条指令，如果是空的也返回空
     * 对于其他类型的指令，原样返回。
     * @param node 需要取头指令的指令
     * @return 头指令
     */
    private AbstractInsnNode getFirstNode(AbstractInsnNode node) {
        if (node instanceof PackageInsnNode) {
            PackageInsnNode packageInsnNode = (PackageInsnNode) node;
            if (!packageInsnNode.getInsnNodes().isEmpty()) {
                return packageInsnNode.getInsnNodes().get(0);
            } else {
                return null;
            }
        } else {
            return node;
        }
    }

    private Set<Integer> selectScc(int index) {
        for (Set<Integer> scc : loops) {
            if (scc.contains(index)) {
                return scc.size() > 1 ? scc : null;
            }
        }
        return null;
    }

    private boolean isAwait(Set<Integer> ssc) {
        for (Integer index : ssc) {
            AbstractInsnNode insnNode = getMv().instructions.get(index);
            if (AsmHelper.isAwait(insnNode)) {
                return true;
            }
        }
        return false;
    }

    private boolean isReturn(BranchAnalyzer.Node<? extends BasicValue> node) {
        if (node.getSuccessors().size() != 1) {
            return false;
        }
        BranchAnalyzer.Node<? extends BasicValue> next = node.getSuccessors().iterator().next();
        AbstractInsnNode insnNode = next.getInsnNode();
        if (insnNode.getOpcode() == Opcodes.ARETURN) {
            return true;
        } else if (insnNode instanceof LabelNode || insnNode instanceof LineNumberNode || insnNode instanceof FrameNode) {
            return isReturn(next);
        } else {
            return false;
        }
    }

    private void pushSuccessors(BranchAnalyzer.Node<? extends BasicValue> node, Deque<WithFlag<BranchAnalyzer.Node<? extends BasicValue>>> stack) {
        BranchAnalyzer.Node<? extends BasicValue>.SuccessorsImpl successors = node.createSuccessors();
        BranchAnalyzer.Node<? extends BasicValue> successor = successors.current();
        while (successor != null) {
            stack.push(WithFlag.of(successor, false));
            successors.next();
            successor = successors.current();
        }
    }

    private void pushSscSuccessors(Set<Integer> scc, Deque<WithFlag<BranchAnalyzer.Node<? extends BasicValue>>> stack) {
        for (Integer idx : scc) {
            BranchAnalyzer.Node<BasicValue> frame = frames[idx];
            for (BranchAnalyzer.Node<? extends BasicValue> successor : frame.getSuccessors()) {
                if (!scc.contains(successor.getIndex())) {
                    stack.push(WithFlag.of(successor, false));
                }
            }
            for (BranchAnalyzer.Node<? extends BasicValue> successor : frame.getTryCatchSuccessors()) {
                if (!scc.contains(successor.getIndex())) {
                    stack.push(WithFlag.of(successor, false));
                }
            }
        }
    }

    private void process(BranchAnalyzer.Node<? extends BasicValue> node) {
        Deque<WithFlag<BranchAnalyzer.Node<? extends BasicValue>>> stack = new ArrayDeque<>();
        stack.push(WithFlag.of(node, false));
        while (!stack.isEmpty()) {
            WithFlag<BranchAnalyzer.Node<? extends BasicValue>> withFlag = stack.pop();
            BranchAnalyzer.Node<? extends BasicValue> root = withFlag.getData();
            int index = root.getIndex();
            AbstractInsnNode insnNode = getMv().instructions.get(index);
            Set<Integer> scc = selectScc(index);
            int label;
            if (scc != null) {
                if (isAwait(scc)) {
                    label = 1;
                } else {
                    label = 2;
                }
            } else if (AsmHelper.isAwait(insnNode)) {
                label = 3;
            } else if (isReturn(root) && !root.getHandlers().isEmpty()) {
                label = 4;
            } else {
                label = 0;
            }
            boolean visited = withFlag.isFlag();
            if (visited) {
                if (insnNodes[index] == null) {
                    if (label == 1) {
                        insnNodes[index] = processAwaitLoopNode(root);
                    } else if (label == 3) {
                        insnNodes[index] = processAwaitNode(root);
                    } else if (label == 4) {
                        insnNodes[index] = processReturnNode(root);
                    } else {
                        insnNodes[index] = insnNode;
                    }
                }
            } else {
                if (label == 2) {
                    pushSscSuccessors(scc, stack);
                    for (Integer idx : scc) {
                        BranchAnalyzer.Node<BasicValue> frame = frames[idx];
                        stack.push(WithFlag.of(frame, true));
                    }
                } else {
                    if (label == 0) {
                        pushSuccessors(root, stack);
                    }
                    stack.push(WithFlag.of(root, true));
                }
            }
        }
    }

    AbstractInsnNode[] collectSuccessors(
            BranchAnalyzer.Node<? extends BasicValue> root,
            BiFunction<AbstractInsnNode, BranchAnalyzer.Node<? extends BasicValue>, AbstractInsnNode> mapper
    ) {
        AbstractInsnNode[] insnNodes = new AbstractInsnNode[getFrames().length];
        Deque<WithFlag<BranchAnalyzer.Node<? extends BasicValue>>> stack = new ArrayDeque<>();
        stack.push(WithFlag.of(root, false));
        while (!stack.isEmpty()) {
            WithFlag<BranchAnalyzer.Node<? extends BasicValue>> withFlag = stack.pop();
            BranchAnalyzer.Node<? extends BasicValue> node = withFlag.getData();
            boolean visited = withFlag.isFlag();
            int index = node.getIndex();
            if (visited) {
                AbstractInsnNode insnNode = getMv().instructions.get(index);
                insnNodes[index] = mapper.apply(insnNode, node);
                // if insnNodes[index] == null, may cause infinite loop.
                assert insnNodes[index] != null;
            } else {
                BranchAnalyzer.Node<? extends BasicValue>.SuccessorsImpl successors = node.createSuccessors();
                BranchAnalyzer.Node<? extends BasicValue> successor = successors.current();
                while (successor != null) {
                    if (insnNodes[successor.getIndex()] == null) {
                        stack.push(WithFlag.of(successor, false));
                    }
                    successors.next();
                    successor = successors.current();
                }
                stack.push(WithFlag.of(node, true));
            }
        }
        return insnNodes;
    }

    String findLambdaName(BranchAnalyzer.Node<? extends BasicValue> node, Arguments arguments, MethodType type) {
        int mappedIndex = mapped(node.getIndex());
        String desc = Type.getMethodDescriptor(Constants.JPROMISE_DESC, arguments.argTypes(0));
        return classContext.findLambdaByHead(getRootMethodContext(), desc, mappedIndex, type);
    }

    MethodNode createLambdaNode(Arguments arguments) {
        int access = Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC;
        if (isStatic()) {
            access |= Opcodes.ACC_STATIC;
        }
        int locals = arguments.argumentSize();
        MethodNode methodNode = new MethodNode(
                Constants.ASM_VERSION,
                access,
                classContext.nextLambdaName(rootMethodName),
                Type.getMethodDescriptor(Constants.JPROMISE_DESC, arguments.argTypes(0)),
                null,
                new String[] { Constants.THROWABLE_NAME }
        );
        methodNode.maxStack = 0;
        methodNode.maxLocals = isStatic() ? locals : (locals + 1);
        return methodNode;
    }

    private String genLocalVarName(String baseName, BranchAnalyzer.Node<? extends BasicValue> frame, int validLocals) {
        if (validLocals < 0) {
            validLocals = calcValidLocals(frame);
        }
        String localVarName = (baseName != null && !baseName.isEmpty()) ? baseName : "__tmp" + this.localVarBase++;
        LocalVar[] localVars = frame.getLocalVars();
        for (int i = 0; i < validLocals; ++i) {
            LocalVar localVar = localVars[i];
            if (localVar != null && Objects.equals(localVar.getName(), localVarName)) {
                return genLocalVarName((baseName != null && !baseName.isEmpty()) ? (baseName + "_") : null, frame, validLocals);
            }
        }
        return localVarName;
    }

    private String calcLocalVarName(int i, BranchAnalyzer.Node<? extends BasicValue> node, int validLocals) {
        LocalVar localVar = node.getLocalVars()[i];
        if (localVar != null) {
            return  localVar.getName();
        }
        return genLocalVarName(null, node, validLocals);
    }

    int calcValidLocals(BranchAnalyzer.Node<? extends BasicValue> node) {
        int locals = node.getLocals();
        AsmHelper.LocalReverseIterator iterator = AsmHelper.reverseIterateLocal(node);
        int i = 0;
        while (iterator.hasNext()) {
            Integer index = iterator.next();
            BasicValue value = node.getLocal(index);
            if (index >= baseValidLocals) {
                if (value instanceof JAsyncValue) {
                    JAsyncValue jAsyncValue = (JAsyncValue) value;
                    if (jAsyncValue.getIndex() == index) {
                        break;
                    }
                }
                i += value.getSize();
            } else {
                if (value == null || value.getType() == null) {
                    ++i;
                } else {
                    break;
                }
            }
        }
        return locals - i;
    }

    private void localsToArguments(int validLocals, BranchAnalyzer.Node<? extends BasicValue> node, Arguments arguments) {
        // locals: this?, x, y, z.
        // x, y, z -> arguments
        int start = isStatic() ? 0 : 1;
        for (int i = start; i < validLocals;) {
            BasicValue value = node.getLocal(i);
            Type type = value.getType();
            String name = calcLocalVarName(i, node, validLocals);
            if (type != null) {
                arguments.addArgument(value, name);
                i += type.getSize();
            } else {
                arguments.addArgument((Type) null, name);
                ++i;
            }
        }
    }

    private void calcExtraAwaitArgumentsType(int validLocals, BranchAnalyzer.Node<? extends BasicValue> node, Arguments arguments) {
        localsToArguments(validLocals, node, arguments);
        int stackSize = node.getStackSize();
        int iMax = stackSize - 1;
        // stack: a, b -> arguments
        for (int i = 0; i < iMax; ++i) {
            BasicValue value = node.getStack(i);
            String name = genLocalVarName(null, node, validLocals);
            arguments.addArgument(value, name);
        }
    }

    private Arguments calcAwaitArgumentsType(int validLocals, BranchAnalyzer.Node<? extends BasicValue> frame) {
        // stack: a, b, promise | locals: this?, x, y, z
        Arguments arguments = new Arguments();
        calcExtraAwaitArgumentsType(validLocals, frame, arguments);
        // await type, throwable type -> arguments
        arguments.addArgument(Constants.OBJECT_DESC, genLocalVarName("awaitResult", frame, validLocals));
        arguments.addArgument(Constants.THROWABLE_DESC, genLocalVarName("error", frame, validLocals));
        arguments.addArgument(Constants.JCONTEXT_DESC, genLocalVarName("context", frame, validLocals));
        // x, y, z, a, b, await type, throwable type, JContext
        return arguments;
    }

    private void restoreStack(List<AbstractInsnNode> insnNodes, BranchAnalyzer.Node<? extends BasicValue> node, int maxLocals, int num) {
        for (int i = 0, iLocal = maxLocals; i < num; ++i) {
            BasicValue value = node.getStack(i);
            if (JAsyncValue.isUninitialized(value)) {
                continue;
            }
            Type type = value.getType();
            if (type != null) {
                iLocal -= type.getSize();
                insnNodes.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), iLocal));
            } else {
                --iLocal;
                insnNodes.add(new VarInsnNode(Opcodes.ALOAD, iLocal));
            }
        }
    }

    private AbstractInsnNode processReturnNode(BranchAnalyzer.Node<? extends BasicValue> node) {
        List<TryCatchBlockNode> tcbNodes = new ArrayList<>();
        int start = -1;
        int end = insnNodes.length;
        for (TryCatchBlockNode handler : node.getHandlers()) {
            int idxStart = mv.instructions.indexOf(handler.start);
            int idxEnd = mv.instructions.indexOf(handler.end);
            if (idxStart >= start && idxEnd <= end) {
                if (idxStart > start || idxEnd < end) {
                    tcbNodes.clear();
                }
                tcbNodes.add(handler);
                start = idxStart;
                end = idxEnd;
            }
        }
        tcbNodes.sort((n1, n2) -> {
            int i1 = mv.instructions.indexOf(n1.handler);
            int i2 = mv.instructions.indexOf(n2.handler);
            return Integer.compare(i1, i2);
        });
        assert !tcbNodes.isEmpty();
        PackageInsnNode packageInsnNode = new PackageInsnNode();
        List<AbstractInsnNode> insnList = packageInsnNode.getInsnNodes();
        // add the insn node just before ARETURN
        // stack: ... -> ..., JPromise
        insnList.add(node.getInsnNode());
        // stack: ..., JPromise
        BranchAnalyzer.Node<? extends BasicValue> returnNode = node.getSuccessors().iterator().next();

        // new Object[tcbNodes.size() * 2]
        // stack: ..., JPromise -> ..., JPromise, int
        insnList.add(AsmHelper.loadConstantInt(tcbNodes.size() * 2));
        // stack: ..., JPromise, int -> ..., JPromise, Object[]
        insnList.add(new TypeInsnNode(Opcodes.ANEWARRAY, Constants.OBJECT_NAME));
        updateStacks(returnNode.getStackSize() + 1);
        // push arguments to array above
        int i = 0;
        for (TryCatchBlockNode tcbNode : tcbNodes) {
            // dup the array instance
            // stack: ..., JPromise, Object[] -> ..., JPromise, Object[], Object[]
            insnList.add(new InsnNode(Opcodes.DUP));
            // push the index to stack
            // stack: ..., JPromise, Object[], Object[] -> ..., JPromise, Object[], Object[], int
            insnList.add(AsmHelper.loadConstantInt(i++));
            // push exception type to stack
            // stack: ..., JPromise, Object[], Object[], int -> ..., JPromise, Object[], Object[], int, Class
            Type exceptionType = tcbNode.type != null ? Type.getObjectType(tcbNode.type) : Constants.THROWABLE_DESC;
            insnList.add(new LdcInsnNode(exceptionType));
            // store the exception type to array
            // stack: ..., JPromise, Object[], Object[], int, Class -> ..., JPromise, Object[]
            insnList.add(new InsnNode(Opcodes.AASTORE));

            // dup the array instance
            // stack: ..., JPromise, Object[] -> ..., JPromise, Object[], Object[]
            insnList.add(new InsnNode(Opcodes.DUP));
            // push the index to stack
            // stack: ..., JPromise, Object[], Object[] -> ..., JPromise, Object[], Object[], int
            insnList.add(AsmHelper.loadConstantInt(i++));
            // push lambda to stack

            int posHandler = mv.instructions.indexOf(tcbNode.handler);
            BranchAnalyzer.Node<BasicValue> handlerNode = getFrames()[posHandler];
            if (!isStatic()) {
                // load this to stack
                // stack: ..., JPromise, Object[], Object[], int -> ..., JPromise, Object[], Object[], int, this
                insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
            }
            int validLocals = calcValidLocals(handlerNode);
            // load validLocals number of local vars except this to stack
            // stack: ..., JPromise, Object[], Object[], int, this? -> ..., JPromise, Object[], Object[], int, this?, x, y, z
            // here use node instead handlerNode because current frame is node. But they should be compatible
            int stackNum = AsmHelper.pushLocalToStack(validLocals, isStatic(), node, insnList);
            updateStacks(returnNode.getStackSize() + 3 + (isStatic() ? 0 : 1) + stackNum);
            // create arguments for lambda
            Arguments arguments = new Arguments();
            localsToArguments(validLocals, node, arguments);
            arguments.addArgument(exceptionType, genLocalVarName("error", node, validLocals));
            arguments.addArgument(Constants.JCONTEXT_DESC, genLocalVarName("context", node, validLocals));
            String lambdaName = findLambdaName(handlerNode, arguments, MethodType.CATCH_BODY);
            if (lambdaName == null) {
                CatchLambdaContext lambdaContext = new CatchLambdaContext(this, arguments, handlerNode, validLocals);
                lambdaContext.buildLambda();
                lambdaName = lambdaContext.getLambdaNode().name;
            }
            // stack: ..., JPromise, Object[], Object[], int, this?, x, y, z -> ..., JPromise, Object[], Object[], int, JAsyncCatchFunction0
            insnList.add(LambdaUtils.invokeJAsyncCatchFunction1(
                    classType(),
                    lambdaName,
                    exceptionType,
                    isStatic(),
                    arguments.argTypes(2)
            ));
            updateStacks(returnNode.getStackSize() + 4);
            // stack: ..., JPromise, Object[], Object[], int, JAsyncCatchFunction0 -> ..., JPromise, Object[]
            insnList.add(new InsnNode(Opcodes.AASTORE));
        }
        // ..., JPROMISE, Object[] -> ..., JPROMISE
        insnList.add(new MethodInsnNode(
                Opcodes.INVOKEINTERFACE,
                Constants.JPROMISE_NAME,
                Constants.JPROMISE_DO_MULTI_CATCHES_NAME,
                Constants.JPROMISE_DO_MULTI_CATCHES1_DESC.getDescriptor())
        );
        insnList.add(new InsnNode(Opcodes.ARETURN));
        packageInsnNode.complete();
        return packageInsnNode;
    }

    private PackageInsnNode processAwaitNode(BranchAnalyzer.Node<? extends BasicValue> node) {
        PackageInsnNode packageInsnNode = new PackageInsnNode();
        List<AbstractInsnNode> insnList = packageInsnNode.getInsnNodes();
        int validLocals = calcValidLocals(node);
        int stackSize = node.getStackSize();
        // stack: promise | locals: this?, x, y, z
        if (stackSize == 1) {
            if (!isStatic()) {
                // load this to stack
                // stack: promise | locals: this, x, y, z -> stack: promise, this | locals: this, x, y, z
                insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
            }
            // push the previous locals to the stack except this
            // stack: promise, this? | locals: this?, x, y, z -> stack: promise, this?, x, y, z | locals: this?, x, y, z
            AsmHelper.pushLocalToStack(validLocals, isStatic(), node, insnList);
        }
        // stack: a, b, promise | locals: this?, x, y, z
        else {
            // store the current stack to the locals (offset by locals). the first one (index of locals) should be the promise
            // stack: a, b, promise | locals: this?, x, y, z -> stack: [] | locals: this?, x, y, z, promise, b, a
            int maxLocals = AsmHelper.storeStackToLocal(validLocals, node, insnList, insnNodes);
            updateLocals(maxLocals);
            // push the target promise to stack
            // stack: [] | locals: this?, x, y, z, promise, b, a -> stack: promise | locals: this?, x, y, z, promise, b, a
            insnList.add(new VarInsnNode(Opcodes.ALOAD, validLocals));
            if (!isStatic()) {
                // load this to stack
                // stack: promise | locals: this, x, y, z, promise, b, a -> stack: promise, this | locals: this, x, y, z, promise, b, a
                insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
            }
            // push the previous locals to the stack except this
            // stack: promise, this? | locals: this?, x, y, z, promise, b, a -> stack: promise, this?, x, y, z | locals: this?, x, y, z, promise, b, a
            AsmHelper.pushLocalToStack(validLocals, isStatic(), node, insnList);
            // push the previous stack from locals to the stack, except the previous stack top, which is the promise.
            // stack: promise, this?, x, y, z | locals: this?, x, y, z, promise, b, a -> stack: promise, this?, x, y, z, a, b | locals: this?, x, y, z, promise, b, a
            restoreStack(insnList, node, maxLocals, stackSize - 1);
        }
        updateStacks(stackSize + validLocals);
        // thenOrCatchLambda: JAsyncPromiseFunction3 = (x, y, z, a, b, Object, throwable, context) -> JPromise
        Arguments arguments = calcAwaitArgumentsType(validLocals, node);
        String lambdaName = findLambdaName(node, arguments, MethodType.AWAIT_BODY);
        if (lambdaName == null) {
            AwaitLambdaContext lambdaContext = new AwaitLambdaContext(this, arguments, node, validLocals);
            lambdaContext.buildLambda();
            lambdaName = lambdaContext.getLambdaNode().name;
        }
        insnList.add(LambdaUtils.invokeJAsyncPromiseFunction3(
                classType(),
                lambdaName,
                Constants.OBJECT_DESC,
                isStatic(),
                arguments.argTypes(3)
        ));
        insnList.add(new MethodInsnNode(
                Opcodes.INVOKEINTERFACE,
                Constants.JPROMISE_NAME,
                Constants.JPROMISE_THEN_OR_CATCH_NAME,
                Constants.JPROMISE_THEN_OR_CATCH1_DESC.getDescriptor())
        );
        insnList.add(new InsnNode(Opcodes.ARETURN));
        packageInsnNode.complete();
        logCollectLocalsAndStack(node, WHY_COLLECT_AWAIT, getMv().name, lambdaName, validLocals);
        return packageInsnNode;
    }

    private int calcLocalsNumByValidLocals(BranchAnalyzer.Node<? extends BasicValue> node, int validLocals) {
        int offset = isStatic() ? 0 : 1;
        int j = 0;
        for (int i = offset; i < validLocals;) {
            BasicValue value = node.getLocal(i);
            ++j;
            if (value != null && value.getType() != null) {
                i += value.getSize();
            } else {
                ++i;
            }
        }
        return j;
    }

    public static final int WHY_COLLECT_AWAIT = 0;
    public static final int WHY_COLLECT_LOOP = 1;
    public static final int WHY_COLLECT_JUMP = 2;
    private static String whyCollect(int whyCollect) {
        if (whyCollect == WHY_COLLECT_AWAIT) {
            return "await";
        } else if (whyCollect == WHY_COLLECT_LOOP) {
            return "loop";
        } else if (whyCollect == WHY_COLLECT_JUMP) {
            return "jump";
        } else {
            return "unknown";
        }
    }

    void collectLocalsAndStackToArrayArg(
            PackageInsnNode packageInsnNode,
            MethodNode methodNode,
            BranchAnalyzer.Node<? extends BasicValue> node,
            AbstractInsnNode[] insnArray,
            int validLocals,
            int whyCollect
    ) {
        int extraStackNum = 0;
        if (whyCollect == WHY_COLLECT_LOOP) {
            extraStackNum = 2;
        } else if (whyCollect == WHY_COLLECT_JUMP) {
            extraStackNum = 1;
        }
        List<AbstractInsnNode> insnNodes = packageInsnNode.getInsnNodes();
        // new Object[numLocals + numStacks]
        // stack: ... -> ..., int
        int stackSize = node.getStackSize();
        insnNodes.add(AsmHelper.loadConstantInt(calcLocalsNumByValidLocals(node, validLocals) + stackSize));
        // stack: ..., int -> ..., Object[]
        insnNodes.add(new TypeInsnNode(Opcodes.ANEWARRAY, Constants.OBJECT_NAME));
        AsmHelper.appendStack(methodNode, node, 1 + extraStackNum);

        // collect locals
        int offset = isStatic() ? 0 : 1;
        int j = 0;
        for (int i = offset; i < validLocals;) {
            // stack: ..., Object[] -> ..., Object[], Object[]
            insnNodes.add(new InsnNode(Opcodes.DUP));
            // stack: ..., Object[], Object[] -> ..., Object[], Object[], int
            insnNodes.add(AsmHelper.loadConstantInt(j++));
            BasicValue value = node.getLocal(i);
            if (value != null && value.getType() != null) {
                Type type = value.getType();
                // stack: ..., Object[], Object[], int -> ..., Object[], Object[], int, T
                insnNodes.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), i));
                if (type.getSort() != Type.OBJECT && type.getSort() != Type.ARRAY) {
                    // stack: ..., Object[], Object[], int, T -> ..., Object[], Object[], int, t
                    primitiveToObject(insnNodes, type);
                }
                i += value.getSize();
            } else {
                // stack: ..., Object[], Object[], int -> ..., Object[], Object[], int, null
                insnNodes.add(new InsnNode(Opcodes.ACONST_NULL));
                ++i;
            }
            // stack: ..., Object[], Object[], int, ? -> ..., Object[]
            insnNodes.add(new InsnNode(Opcodes.AASTORE));
            AsmHelper.appendStack(methodNode, node, 4 + extraStackNum);
        }

        // collect stacks
        for (int i = 0; i < stackSize; ++i) {
            BasicValue value = node.getStack(stackSize - i - 1);
            if (value instanceof JAsyncValue) {
                JAsyncValue asyncValue = (JAsyncValue) value;
                if (asyncValue.isUninitialized()) {
                    if (insnArray != null) {
                        AsmHelper.removeNewInsnNodes(insnArray, asyncValue, node);
                    }
                    continue;
                }
            }
            if (value != null && value.getType() != null) {
                Type type = value.getType();
                if (type.getSize() == 1) {
                    // stack: ..., ?, Object[] -> ..., Object[], ?, Object[]
                    insnNodes.add(new InsnNode(Opcodes.DUP_X1));
                    // stack: ..., Object[], ?, Object[] -> ..., Object[], Object[], ?
                    insnNodes.add(new InsnNode(Opcodes.SWAP));
                    // stack: ..., Object[], Object[], ? -> ..., Object[], Object[], ?, int
                    insnNodes.add(AsmHelper.loadConstantInt(validLocals + stackSize - 1 - i));
                    // stack: ..., Object[], Object[], ?, int -> ..., Object[], Object[], int, ?
                    insnNodes.add(new InsnNode(Opcodes.SWAP));
                } else {
                    // stack: ..., ?, Object[] -> ..., ?
                    // locals: ... -> ..., Object[]
                    insnNodes.add(new VarInsnNode(Opcodes.ASTORE, validLocals));
                    // stack: ..., ? -> ...
                    // locals: ..., Object[] -> ..., Object[], ?
                    insnNodes.add(new VarInsnNode(Opcodes.ASTORE, validLocals + 1));
                    // stack: ... -> ..., Object[]
                    insnNodes.add(new VarInsnNode(Opcodes.ALOAD, validLocals));
                    // stack: ... -> ..., Object[], Object[]
                    insnNodes.add(new InsnNode(Opcodes.DUP));
                    // stack: ... -> ..., Object[], Object[], int
                    insnNodes.add(AsmHelper.loadConstantInt(validLocals + stackSize - 1 - i));
                    // stack: ... -> ..., Object[], Object[], int, ?
                    insnNodes.add(new VarInsnNode(Opcodes.ALOAD, validLocals + 1));
                    AsmHelper.appendLocal(methodNode, node, 3);
                }
                if (type.getSort() != Type.OBJECT && type.getSort() != Type.ARRAY) {
                    // stack: ..., Object[], Object[], int, t -> ..., Object[], Object[], int, T
                    primitiveToObject(insnNodes, type);
                }
            } else {
                // stack: ..., ?, Object[] -> ..., Object[], ?, Object[]
                insnNodes.add(new InsnNode(Opcodes.DUP_X1));
                // stack: ..., Object[], ?, Object[] -> ..., Object[], Object[], ?
                insnNodes.add(new InsnNode(Opcodes.SWAP));
                // stack: ..., Object[], Object[], ? -> ..., Object[], Object[], ?, int
                insnNodes.add(AsmHelper.loadConstantInt(validLocals + stackSize - 1 - i));
                // stack: ..., Object[], Object[], ?, int -> ..., Object[], Object[], int, ?
                insnNodes.add(new InsnNode(Opcodes.SWAP));
            }
            // stack: ..., Object[], Object[], int, ? -> ..., Object[]
            insnNodes.add(new InsnNode(Opcodes.AASTORE));
            AsmHelper.appendStack(methodNode, node, 4 + extraStackNum);
        }
    }

    void logCollectLocalsAndStack(BranchAnalyzer.Node<? extends BasicValue> node, int whyCollect, String fromMethod, String toMethod, int validLocals) {
        if (!info.isLogLocalsAndStackInfo()) {
            return;
        }
        List<BasicValue> currentLocals = new ArrayList<>();
        List<BasicValue> collectedLocals = new ArrayList<>();
        List<BasicValue> currentStacks = new ArrayList<>();
        List<BasicValue> collectedStacks = new ArrayList<>();
        int offset = isStatic() ? 0 : 1;
        for (int i = offset; i < node.getLocals();) {
            BasicValue value = node.getLocal(i);
            currentLocals.add(value);
            i += value != null ? value.getSize() : 1;
        }
        for (int i = offset; i < validLocals;) {
            BasicValue value = node.getLocal(i);
            if (value != null && value.getType() != null) {
                i += value.getSize();
            } else {
                ++i;
            }
            collectedLocals.add(value);
        }
        for (int i = 0; i < node.getStackSize(); ++i) {
            currentStacks.add(node.getStack(i));
        }
        int stackNum = whyCollect == WHY_COLLECT_AWAIT ? (node.getStackSize() - 1) : node.getStackSize();
        for (int i = 0; i < stackNum; ++i) {
            BasicValue value = node.getStack(i);
            if (JAsyncValue.isUninitialized(value)) {
                collectedStacks.add(BasicValue.UNINITIALIZED_VALUE);
            } else {
                collectedStacks.add(value);
            }
        }
        String prefix = "[" + fromMethod + " -> " + toMethod + "][" + whyCollect(whyCollect) + "](" + mapped(node.getIndex()) + "/" + baseValidLocals + "): ";
        Logger.info(prefix + "current locals { " + currentLocals.stream().map(BasicValue::toString).collect(Collectors.joining(", ")) + " } ");
        Logger.info(prefix + "collect locals { " + collectedLocals.stream().map(BasicValue::toString).collect(Collectors.joining(", ")) + " }");
        Logger.info(prefix + "current stacks { " + currentStacks.stream().map(BasicValue::toString).collect(Collectors.joining(", ")) + " }");
        Logger.info(prefix + "collect stacks { " + collectedStacks.stream().map(BasicValue::toString).collect(Collectors.joining(", ")) + " }");
    }

    String genJumpIndexField(int mappedIndex) {
        String fieldName = classContext.generateUniqueFieldName("JUMP_TARGET_" + rootMethodName + "_" + mappedIndex);
        if (classContext.hasFieldContext(getRootMethodContext(), fieldName)) {
            return fieldName;
        }
        FieldContext fieldContext = FieldContext.createPrivateStaticFinalField(fieldName, Type.INT_TYPE.getDescriptor());
        fieldContext.addInitInsnNode(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                Constants.JPROMISE_NAME,
                Constants.JPROMISE_GEN_ID_NAME,
                Constants.JPROMISE_GEN_ID_DESC.getDescriptor(),
                true)
        );
        fieldContext.addInitInsnNode(new FieldInsnNode(Opcodes.PUTSTATIC, classContext.getInternalName(), fieldName, fieldContext.getDescriptor()));
        fieldContext.addInitInsnNode(new InsnNode(Opcodes.RETURN));
        fieldContext.updateMaxStack(1);
        classContext.addFieldContext(getRootMethodContext(), fieldContext);
        return fieldName;
    }

    AbstractInsnNode loadJumpTarget(String jumpTargetFieldName) {
        return new FieldInsnNode(Opcodes.GETSTATIC, classContext.getInternalName(), jumpTargetFieldName, Type.INT_TYPE.getDescriptor());
    }

    private PackageInsnNode processAwaitLoopNode(BranchAnalyzer.Node<? extends BasicValue> node) {
        int mappedIndex = mapped(node.getIndex());
        PackageInsnNode packageInsnNode = new PackageInsnNode();
        List<AbstractInsnNode> insnNodes = packageInsnNode.getInsnNodes();
        insnNodes.add(node.getInsnNode());
        // return JPromise.portal(localVars -> {...}, jumpIndex, localVars)
        // push localVars -> {...} to stack
        if (!isStatic()) {
            // push this to stack
            // stack: ... -> ..., this
            insnNodes.add(new VarInsnNode(Opcodes.ALOAD, 0));
            AsmHelper.appendStack(getMv(), node, 1);
        }
        String jumpTargetFieldName = genJumpIndexField(mappedIndex);
        int validLocals = calcValidLocals(node);
        Arguments arguments = Arguments.of(
                Constants.OBJECT_ARRAY_DESC, genLocalVarName("arguments", node, validLocals),
                Constants.JCONTEXT_DESC, genLocalVarName("context", node, validLocals)
        );
        String lambdaName = findLambdaName(node, arguments, MethodType.LOOP_BODY);
        if (lambdaName == null) {
            LoopLambdaContext loopLambdaContext = new LoopLambdaContext(this, arguments, node, jumpTargetFieldName);
            loopLambdaContext.buildLambda();
            lambdaName = loopLambdaContext.getLambdaNode().name;
        }
        // stack: ..., this? -> ..., localVars -> {...}
        insnNodes.add(LambdaUtils.invokeJAsyncPortalTask(
                classType(),
                lambdaName,
                isStatic()
        ));
        // stack: ..., localVars -> {...} -> ..., localVars -> {...}, jumpIndex
        insnNodes.add(loadJumpTarget(jumpTargetFieldName));
        // stack: ..., localVars -> {...}, jumpIndex -> ..., localVars -> {...}, jumpIndex, localVars
        collectLocalsAndStackToArrayArg(packageInsnNode, getMv(), node, this.insnNodes, validLocals, WHY_COLLECT_LOOP);
        // stack: ..., localVars -> {...}, jumpIndex, localVars -> ..., JPromise
        insnNodes.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                Constants.JPROMISE_NAME,
                Constants.JPROMISE_PORTAL_NAME,
                Constants.JPROMISE_PORTAL0_DESC.getDescriptor(),
                true
        ));
        insnNodes.add(new InsnNode(Opcodes.ARETURN));
        packageInsnNode.complete();
        logCollectLocalsAndStack(node, WHY_COLLECT_LOOP, getMv().name, lambdaName, validLocals);
        return packageInsnNode;
    }

    int mapped(int index) {
        return map != null ? map.get(index) : index;
    }

    public enum MethodType {
        TOP,
        AWAIT_BODY,
        LOOP_BODY,
        CATCH_BODY,
    }
}
