package io.github.vipcxj.jasync.ng.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;

import java.util.*;
import java.util.function.BiFunction;

import static io.github.vipcxj.jasync.ng.asm.AsmHelper.primitiveToObject;
import static io.github.vipcxj.jasync.ng.asm.Constants.THROWABLE_DESC;
import static io.github.vipcxj.jasync.ng.asm.Utils.addManyMap;

public class MethodContext {
    private final ClassContext classContext;
    private final MethodNode mv;
    private final JAsyncInfo info;
    private final BranchAnalyzer.Node<BasicValue>[] frames;
    private final List<Integer> localsToUpdate;
    private final List<Integer> stacksToUpdate;
    private final MethodType type;
    private final int head;
    private int index = 0;
    private List<Integer> map;

    protected final MethodContext parent;
    private final List<MethodContext> children;
    private final AbstractInsnNode[] insnNodes;
    private List<Set<Integer>> loops;

    public MethodContext(ClassContext classContext, MethodNode mv) {
        this(classContext, mv, null, MethodType.TOP, null);
    }

    public MethodContext(ClassContext classContext, MethodNode mv, List<Integer> map, MethodType type, MethodContext parent) {
        this.classContext = classContext;
        this.mv = mv;
        this.map = map;
        this.info = parent != null ? parent.getInfo() : JAsyncInfo.of(mv);
        this.localsToUpdate = new ArrayList<>();
        this.stacksToUpdate = new ArrayList<>();
        this.type = type;
        this.head = mapped(0);
        BranchAnalyzer analyzer = new BranchAnalyzer();
        try {
            analyzer.analyzeAndComputeMaxs(classContext.getName(), mv);
            this.frames = analyzer.getNodes();
        } catch (AnalyzerException e) {
            AsmHelper.printFrameProblem(
                    classContext.getName(),
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
        this.children = new ArrayList<>();
        insnNodes = new AbstractInsnNode[this.frames.length];
    }

    public MethodContext createChild(MethodNode methodNode, List<Integer> map, MethodType type) {
        return new MethodContext(classContext, methodNode, map, type, this);
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

    public String nextLambdaName() {
        if (parent != null) {
            return parent.nextLambdaName();
        } else {
            String name = createLambdaName();
            while (classContext.containMethod(name)) {
                name = createLambdaName();
            }
            return name;
        }
    }

    private String createLambdaName() {
        return "lambda$" + getMv().name + "$" + index++;
    }

    public Type classType() {
        return Type.getObjectType(classContext.getName());
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

    public void addLambdaContext(MethodNode lambdaNode, List<Integer> map, MethodType type) {
        MethodContext childContext = createChild(lambdaNode, map, type);
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
            map = newMap;
            getMv().instructions = newInsnList;
            getMv().tryCatchBlocks = completedTcbNodes;
            getMv().localVariables = localVariableNodes;
            updateMax();
            for (MethodContext child : children) {
                child.process();
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
        } else if (insnNode instanceof LabelNode || insnNode instanceof LineNumberNode) {
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
                if (label == 1) {
                    insnNodes[index] = processAwaitLoopNode(root);
                } else if (label == 3) {
                    insnNodes[index] = processAwaitNode(root);
                } else if (label == 4) {
                    insnNodes[index] = processReturnNode(root);
                } else {
                    insnNodes[index] = insnNode;
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
                nextLambdaName(),
                Type.getMethodDescriptor(Constants.JPROMISE_DESC, arguments.argTypes(0)),
                null,
                new String[] { Constants.THROWABLE_NAME }
        );
        methodNode.maxStack = 0;
        methodNode.maxLocals = isStatic() ? locals : (locals + 1);
        return methodNode;
    }

    private int calcValidLocals(BranchAnalyzer.Node<? extends BasicValue> node) {
        int locals = node.getLocals();
        AsmHelper.LocalReverseIterator iterator = AsmHelper.reverseIterateLocal(node);
        int i = 0;
        // if in loop body, ignore last local var: stack
        // if in await body, ignore last local var: error
        if (type == MethodType.LOOP_BODY || type == MethodType.AWAIT_BODY) {
            ++i;
            iterator.next();
        }
        while (iterator.hasNext()) {
            Integer index = iterator.next();
            BasicValue value = node.getLocal(index);
            if (value == null || value.getType() == null) {
                ++i;
            } else {
                break;
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
        arguments.addArgument(THROWABLE_DESC);
        // x, y, z, a, b, await type, throwable type
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
            Type exceptionType = tcbNode.type != null ? Type.getObjectType(tcbNode.type) : THROWABLE_DESC;
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
            String lambdaName = findLambdaName(handlerNode, arguments, MethodType.CATCH_BODY);
            if (lambdaName == null) {
                CatchLambdaContext lambdaContext = new CatchLambdaContext(this, arguments, handlerNode);
                lambdaContext.buildLambda();
                lambdaName = lambdaContext.getLambdaNode().name;
            }
            // stack: ..., JPromise, Object[], Object[], int, this?, x, y, z -> ..., JPromise, Object[], Object[], int, JAsyncCatchFunction0
            insnList.add(LambdaUtils.invokeJAsyncCatchFunction0(
                    classType(),
                    lambdaName,
                    exceptionType,
                    isStatic(),
                    arguments.argTypes(1)
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
        // thenOrCatchLambda: JAsyncPromiseFunction2 = (x, y, z, a, b, Object, throwable) -> JPromise
        Arguments arguments = calcAwaitArgumentsType(validLocals, node);
        String lambdaName = findLambdaName(node, arguments, MethodType.AWAIT_BODY);
        if (lambdaName == null) {
            AwaitLambdaContext lambdaContext = new AwaitLambdaContext(this, arguments, node, validLocals);
            lambdaContext.buildLambda();
            lambdaName = lambdaContext.getLambdaNode().name;
        }
        insnList.add(LambdaUtils.invokeJAsyncPromiseFunction2(
                classType(),
                lambdaName,
                Constants.OBJECT_DESC,
                isStatic(),
                arguments.argTypes(2)
        ));
        insnList.add(new MethodInsnNode(
                Opcodes.INVOKEINTERFACE,
                Constants.JPROMISE_NAME,
                Constants.JPROMISE_THEN_OR_CATCH_NAME,
                Constants.JPROMISE_THEN_OR_CATCH1_DESC.getDescriptor())
        );
        insnList.add(new InsnNode(Opcodes.ARETURN));
        packageInsnNode.complete();
        return packageInsnNode;
    }

    private void push(List<AbstractInsnNode> insnNodes) {
        insnNodes.add(new MethodInsnNode(
                Opcodes.INVOKEINTERFACE,
                Constants.JPUSH_CONTEXT_NAME,
                Constants.JPUSH_CONTEXT_PUSH_NAME,
                Constants.JPUSH_CONTEXT_PUSH_DESC.getDescriptor(),
                true
        ));
    }

    void pushStack(PackageInsnNode packageInsnNode, MethodNode methodNode, BranchAnalyzer.Node<? extends BasicValue> node, AbstractInsnNode[] insnArray) {
        List<AbstractInsnNode> insnNodes = packageInsnNode.getInsnNodes();
        // stack: ... -> ..., pusher
        insnNodes.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                Constants.JCONTEXT_NAME,
                Constants.JCONTEXT_CREATE_STACK_PUSHER_NAME,
                Constants.JCONTEXT_CREATE_STACK_PUSHER_DESC.getDescriptor(),
                true
        ));
        AsmHelper.appendStack(methodNode, node, 1);

        int locals = node.getLocals();
        int stacks = node.getStackSize();
        for (int i = 0; i < stacks; ++i) {
            BasicValue value = node.getStack(stacks - i - 1);
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
                    // stack: ..., t, pusher -> ..., pusher, t
                    insnNodes.add(new InsnNode(Opcodes.SWAP));
                } else {
                    // stack: ..., t, pusher -> ..., t
                    // locals: ..., -> ..., pusher
                    insnNodes.add(new VarInsnNode(Opcodes.ASTORE, locals));
                    // stack: ..., t -> ...
                    // locals: ..., pusher -> ..., pusher, t
                    insnNodes.add(new VarInsnNode(type.getOpcode(Opcodes.ISTORE), locals + 1) );
                    // stack: ... -> ..., pusher
                    insnNodes.add(new VarInsnNode(Opcodes.ALOAD, locals));
                    // stack: ... -> ..., pusher, t
                    insnNodes.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), locals + 1));
                    AsmHelper.appendLocal(methodNode, node, 2);
                }
                if (type.getSort() != Type.OBJECT && type.getSort() != Type.ARRAY) {
                    primitiveToObject(insnNodes, type);
                }
            } else {
                // stack: ..., t, pusher -> ..., pusher, t
                insnNodes.add(new InsnNode(Opcodes.SWAP));
            }
            // pusher.push(t)
            // stack: ..., pusher, t -> ..., pusher
            push(insnNodes);
        }

        // stack: pusher
        AsmHelper.LocalReverseIterator iterator = AsmHelper.reverseIterateLocal(node);
        boolean first = true;
        while (iterator.hasNext()) {
            int pos = iterator.next();
            // this should not be push.
            if (isStatic() || pos > 0) {
                if (first) {
                    first = false;
                    // The last local var in loop method is stack, it need not be pushed.
                    // The last local var in await method is error, it need not be pushed as well.
                    if (type == MethodType.LOOP_BODY || type == MethodType.AWAIT_BODY) {
                        continue;
                    }
                }
                BasicValue value = node.getLocal(pos);
                if (value != null && value.getType() != null) {
                    Type type = value.getType();
                    // stack: pusher -> pusher, t
                    insnNodes.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), pos));
                    if (type.getSort() != Type.OBJECT && type.getSort() != Type.ARRAY) {
                        // stack: pusher, t -> pusher, T
                        primitiveToObject(insnNodes, type);
                    }
                } else {
                    // stack: pusher -> pusher, null
                    insnNodes.add(new InsnNode(Opcodes.ACONST_NULL));
                }
                AsmHelper.updateStack(methodNode, 2);
                // pusher.push(t)
                // stack: pusher, t -> pusher
                push(insnNodes);
            }
        }

        // pusher.complete()
        // stack: pusher -> promise
        insnNodes.add(new MethodInsnNode(
                Opcodes.INVOKEINTERFACE,
                Constants.JPUSH_CONTEXT_NAME,
                Constants.JPUSH_CONTEXT_COMPLETE_NAME,
                Constants.JPUSH_CONTEXT_COMPLETE_DESC.getDescriptor(),
                true
        ));
    }

    private void pushThenImmediate(MethodNode methodNode, PackageInsnNode packageInsnNode, MethodNode lambdaNode) {
        List<AbstractInsnNode> insnNodes = packageInsnNode.getInsnNodes();
        if (!isStatic()) {
            // stack: promise -> promise, this
            insnNodes.add(new VarInsnNode(Opcodes.ALOAD, 0));
            AsmHelper.updateStack(methodNode, 2);
        }
        // push lambda
        // stack: promise, this? -> promise, this?, JAsyncPromiseSupplier0
        insnNodes.add(LambdaUtils.invokeJAsyncPromiseSupplier0(
                classType(),
                lambdaNode.name,
                isStatic()
        ));
        AsmHelper.updateStack(methodNode, isStatic() ? 2 : 3);
        // promise.thenImmediate(supplier)
        // stack: promise, this?, JAsyncPromiseSupplier0 -> promise
        insnNodes.add(new MethodInsnNode(
                Opcodes.INVOKEINTERFACE,
                Constants.JPROMISE_NAME,
                Constants.JPROMISE_THEN_IMMEDIATE_NAME,
                Constants.JPROMISE_THEN_IMMEDIATE0_DESC.getDescriptor(),
                true
        ));
        // return promise
        // stack promise -> []
        insnNodes.add(new InsnNode(Opcodes.ARETURN));
    }

    private PackageInsnNode processAwaitLoopNode(BranchAnalyzer.Node<? extends BasicValue> node) {
        // JContext.createStackPusher()
        //   .push(var0).push(var1)....push(varN)
        //   .complete()
        //   .thenImmediate(outLambda)

        // () -> JPromise.portal(midLambda)
        MethodNode outLambda = createLambdaNode(Arguments.EMPTY);
        // (portal) -> JContext.popStack(innerLambda)
        MethodNode midLambda = createLambdaNode(Arguments.of(Constants.JPORTAL_DESC));

        if (!isStatic()) {
            outLambda.visitVarInsn(Opcodes.ALOAD, 0);
            AsmHelper.updateStack(outLambda, 1);
        }
        outLambda.instructions.add(LambdaUtils.invokeJAsyncPortalTask(
                classType(),
                midLambda.name,
                isStatic()
        ));
        outLambda.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                Constants.JPROMISE_NAME,
                Constants.JPROMISE_PORTAL_NAME,
                Constants.JPROMISE_PORTAL0_DESC.getDescriptor(),
                true
        );
        outLambda.visitInsn(Opcodes.ARETURN);
        List<Integer> outLambdaMap = new ArrayList<>();
        addManyMap(outLambdaMap, mapped(node.getIndex()), outLambda.instructions.size());
        addLambdaContext(outLambda, outLambdaMap, MethodType.LOOP_OUT);

        if (!isStatic()) {
            // push this to stack
            // stack: [] -> this
            midLambda.visitVarInsn(Opcodes.ALOAD, 0);
            AsmHelper.updateStack(midLambda, 1);
        }
        // push portal to stack.
        // stack: this? -> this?, portal
        midLambda.visitVarInsn(Opcodes.ALOAD, isStatic() ? 0 : 1);
        AsmHelper.updateStack(midLambda, isStatic() ? 1 : 2);

        Arguments arguments = Arguments.of(Constants.JPORTAL_DESC, Constants.JSTACK_DESC);
        String innerLambdaName = findLambdaName(node, arguments, MethodType.LOOP_BODY);
        if (innerLambdaName == null) {
            LoopLambdaContext loopLambdaContext = new LoopLambdaContext(this, arguments, node);
            loopLambdaContext.buildLambda();
            innerLambdaName = loopLambdaContext.getLambdaNode().name;
        }
        midLambda.instructions.add(LambdaUtils.invokeJAsyncPromiseFunction0(
                classType(),
                innerLambdaName,
                Constants.JSTACK_DESC,
                isStatic(),
                Constants.JPORTAL_DESC
        ));
        midLambda.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                Constants.JCONTEXT_NAME,
                Constants.JCONTEXT_POP_STACK_NAME,
                Constants.JCONTEXT_POP_STACK_DESC.getDescriptor(),
                true
        );
        midLambda.visitInsn(Opcodes.ARETURN);
        List<Integer> midLambdaMap = new ArrayList<>();
        addManyMap(midLambdaMap, mapped(node.getIndex()), midLambda.instructions.size());
        addLambdaContext(midLambda, midLambdaMap, MethodType.LOOP_MIDDLE);
        PackageInsnNode packageInsnNode = new PackageInsnNode();
        packageInsnNode.getInsnNodes().add(node.getInsnNode());
        pushStack(packageInsnNode, getMv(), node, insnNodes);
        pushThenImmediate(getMv(), packageInsnNode, outLambda);
        packageInsnNode.complete();
        return packageInsnNode;
    }

    int mapped(int index) {
        return map != null ? map.get(index) : index;
    }

    public enum MethodType {
        TOP,
        AWAIT_BODY,
        LOOP_BODY,
        LOOP_OUT,
        LOOP_MIDDLE,
        CATCH_BODY,
    }
}
