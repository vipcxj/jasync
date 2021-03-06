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

    public MethodContext(ClassContext classContext, MethodNode mv, String rootMethodName) {
        // The root baseValidLocals = AsmHelper.calcMethodArgLocals(mv) - 1
        // Because the last argument is JContext which should be ignored.
        this(classContext, mv, null, MethodType.TOP, null, rootMethodName, AsmHelper.calcMethodArgLocals(mv) - 1);
    }

    public MethodContext(
            ClassContext classContext,
            MethodNode mv,
            List<Integer> map,
            MethodType type,
            MethodContext parent,
            String rootMethodName,
            int baseValidLocals
    ) {
        this.classContext = classContext;
        this.mv = mv;
        this.contextVarIndex = AsmHelper.calcMethodArgLocals(mv) - 1;
        this.map = map;
        this.info = parent != null ? parent.getInfo() : JAsyncInfo.of(mv);
        this.localsToUpdate = new ArrayList<>();
        this.stacksToUpdate = new ArrayList<>();
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
                    analyzer.getFrames(),
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
        return new MethodContext(classContext, methodNode, map, type, this, rootMethodName, parentLocals);
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
        return (mv.access & Opcodes.ACC_STATIC) != 0;
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
            Arrays.fill(insnNodes, null);
            process(nodes[0]);
            clearInsnList(getMv().instructions);
            List<LocalVariableNode> localVariableNodes = new ArrayList<>();
            LocalVariableNode[] localVariableArray = new LocalVariableNode[mv.maxLocals];
            List<TryCatchBlockNode> processingTcbNodes = new ArrayList<>();
            List<TryCatchBlockNode> completedTcbNodes = new ArrayList<>();
            InsnList newInsnList = new InsnList();
            List<Integer> newMap = new ArrayList<>();
            for (int i = 0; i < insnNodes.length; ++i) {
                AbstractInsnNode insnNode = insnNodes[i];
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
                        for (int j = 0; j < localVariableArray.length; ++j) {
                            LocalVariableNode variableNode = localVariableArray[j];
                            if (variableNode != null && variableNode.start != null) {
                                variableNode.end = packageInsnNode.getEndNode();
                            }
                            pushLocalVariable(localVariableArray, j, localVariableNodes, null);
                        }
                    } else {
                        newMap.add(mappedIndex);
                        newInsnList.add(insnNode);
                        updateLocalVar(localVariableArray, localVariableNodes, insnNode, frame);
                    }
                }
            }
            LabelNode endNode;
            if (newInsnList.getLast() instanceof LabelNode) {
                endNode = (LabelNode) newInsnList.getLast();
                completeLocalVar(localVariableArray, localVariableNodes, null, true);
            } else {
                endNode = new LabelNode();
                newInsnList.add(endNode);
                completeLocalVar(localVariableArray, localVariableNodes, endNode, true);
            }
            completeTryCatchBlockNodes(processingTcbNodes, completedTcbNodes, endNode);
            filterTryCatchBlockNodes(completedTcbNodes, newInsnList);
            map = newMap;
            getMv().instructions = newInsnList;
            getMv().tryCatchBlocks = completedTcbNodes;
            getMv().localVariables = localVariableNodes;
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
                && equals(localVar.getSignature(), localVariableNode.signature);
    }

    private void pushLocalVariable(LocalVariableNode[] localVariableArray, int i, List<LocalVariableNode> localVariableList, LabelNode endNode) {
        LocalVariableNode localVariableNode = localVariableArray[i];
        if (localVariableNode != null && localVariableNode.start != null && (localVariableNode.end != null || endNode != null)) {
            if (localVariableNode.end == null) {
                localVariableNode.end = endNode;
            }
            localVariableList.add(localVariableNode);
        }
        localVariableArray[i] = null;
    }

    void updateLocalVar(
            LocalVariableNode[] localVariableArray,
            List<LocalVariableNode> localVariableList,
            AbstractInsnNode insnNode, BranchAnalyzer.Node<? extends BasicValue> frame
    ) {
        LocalVar[] localVars = frame.getLocalVars();
        int size = Math.min(localVariableArray.length, localVars.length);
        for (int i = 0; i < size; ++i) {
            LocalVar localVar = localVars[i];
            LocalVariableNode localVariableNode = localVariableArray[i];
            if (!equals(localVar, localVariableNode)) {
                if (localVar == null) {
                    pushLocalVariable(localVariableArray, i, localVariableList, null);
                } else if (localVariableNode == null) {
                    LocalVariableNode variableNode = new LocalVariableNode(localVar.getName(), localVar.getDesc(), localVar.getSignature(), null, null, i);
                    if (insnNode instanceof LabelNode) {
                        variableNode.start = (LabelNode) insnNode;
                    }
                    localVariableArray[i] = variableNode;
                } else {
                    pushLocalVariable(localVariableArray, i, localVariableList, null);
                    LocalVariableNode variableNode = new LocalVariableNode(localVar.getName(), localVar.getDesc(), localVar.getSignature(), null, null, i);
                    if (insnNode instanceof LabelNode) {
                        variableNode.start = (LabelNode) insnNode;
                    }
                    localVariableArray[i] = variableNode;
                }
            } else if (localVariableNode != null && insnNode instanceof LabelNode) {
                if (localVariableNode.start == null) {
                    localVariableNode.start = (LabelNode) insnNode;
                } else {
                    localVariableNode.end = (LabelNode) insnNode;
                }
            }
        }
    }

    void completeLocalVar(LocalVariableNode[] localVariableArray, List<LocalVariableNode> localVariableList, LabelNode endNode, boolean includeThis) {
        for (int i = 0; i < localVariableArray.length; ++i) {
            LocalVariableNode localVariableNode = localVariableArray[i];
            if (localVariableNode != null && (includeThis || isStatic() || localVariableNode.index != 0)) {
                pushLocalVariable(localVariableArray, i, localVariableList, endNode);
            }
        }
    }

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
            if (type != null) {
                arguments.addArgument(value);
                i += type.getSize();
            } else {
                arguments.addArgument((Type) null);
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
            arguments.addArgument(value);
        }
    }

    private Arguments calcAwaitArgumentsType(int validLocals, BranchAnalyzer.Node<? extends BasicValue> frame) {
        // stack: a, b, promise | locals: this?, x, y, z
        Arguments arguments = new Arguments();
        calcExtraAwaitArgumentsType(validLocals, frame, arguments);
        // await type, throwable type -> arguments
        arguments.addArgument(Constants.OBJECT_DESC);
        arguments.addArgument(Constants.THROWABLE_DESC);
        arguments.addArgument(Constants.JCONTEXT_DESC);
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
            arguments.addArgument(exceptionType);
            arguments.addArgument(Constants.JCONTEXT_DESC);
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
        Arguments arguments = Arguments.of(Constants.OBJECT_ARRAY_DESC, Constants.JCONTEXT_DESC);
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
        int validLocals = calcValidLocals(node);
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
