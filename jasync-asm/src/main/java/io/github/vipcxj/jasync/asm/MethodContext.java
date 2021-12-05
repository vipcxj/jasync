package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;

import java.util.*;
import java.util.function.BiFunction;

public class MethodContext {
    private final ClassContext classContext;
    private final MethodNode mv;
    private final JAsyncInfo info;
    private final BranchAnalyzer.Node<BasicValue>[] frames;
    private final List<Integer> localsToUpdate;
    private final List<Integer> stacksToUpdate;
    private final boolean loop;
    private final int head;
    private final boolean hide;
    private int index = 0;
    private List<Integer> map;

    protected final MethodContext parent;
    private final List<MethodContext> children;
    private final AbstractInsnNode[] insnNodes;
    private List<Set<Integer>> loops;

    public MethodContext(ClassContext classContext, MethodNode mv) {
        this(classContext, mv, null, false, false, null);
    }

    public MethodContext(ClassContext classContext, MethodNode mv, List<Integer> map, boolean loop, boolean hide, MethodContext parent) {
        this.classContext = classContext;
        this.mv = mv;
        this.map = map;
        this.info = parent != null ? parent.getInfo() : JAsyncInfo.of(mv);
        this.localsToUpdate = new ArrayList<>();
        this.stacksToUpdate = new ArrayList<>();
        this.loop = loop;
        this.hide = hide;
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

    public MethodContext createChild(MethodNode methodNode, List<Integer> map, boolean loop, boolean hide) {
        return new MethodContext(classContext, methodNode, map, loop, hide, this);
    }

    public MethodNode getMv() {
        return mv;
    }

    public List<Integer> getMap() {
        return map;
    }

    public int getHead() {
        return head;
    }

    public boolean isHide() {
        return hide;
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

    public void addLambdaContext(MethodNode lambdaNode, List<Integer> map, boolean loop, boolean hide) {
        MethodContext childContext = createChild(lambdaNode, map, loop, hide);
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
            InsnList newInsnList = new InsnList();
            List<Integer> newMap = new ArrayList<>();
            for (int i = 0; i < insnNodes.length; ++i) {
                AbstractInsnNode insnNode = insnNodes[i];
                if (insnNode != null) {
                    int mappedIndex = mapped(i);
                    BranchAnalyzer.Node<BasicValue> frame = frames[i];
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
            if (!(newInsnList.getLast() instanceof LabelNode)) {
                LabelNode endNode = new LabelNode();
                newInsnList.add(endNode);
                completeLocalVar(localVariableArray, localVariableNodes, endNode, true);
            } else {
                completeLocalVar(localVariableArray, localVariableNodes, null, true);
            }
            map = newMap;
            getMv().instructions = newInsnList;
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

    private void updateLocalVar(LocalVariableNode[] localVariableArray, List<LocalVariableNode> localVariableList, AbstractInsnNode insnNode, BranchAnalyzer.Node<? extends BasicValue> frame) {
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

    private void completeLocalVar(LocalVariableNode[] localVariableArray, List<LocalVariableNode> localVariableList, LabelNode endNode, boolean includeThis) {
        for (int i = 0; i < localVariableArray.length; ++i) {
            LocalVariableNode localVariableNode = localVariableArray[i];
            if (localVariableNode != null && (includeThis || isStatic() || localVariableNode.index != 0)) {
                pushLocalVariable(localVariableArray, i, localVariableList, endNode);
            }
        }
    }

    private LocalVariableNode findThisVar(LocalVariableNode[] localVariableArray, boolean isStatic) {
        if (isStatic)
            return null;
        for (LocalVariableNode localVariableNode : localVariableArray) {
            if (localVariableNode != null && localVariableNode.index == 0) {
                return localVariableNode;
            }
        }
        return null;
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

    private void pushSuccessors(BranchAnalyzer.Node<? extends BasicValue> node, Deque<WithFlag<BranchAnalyzer.Node<? extends BasicValue>>> stack) {
        BranchAnalyzer.Node<? extends BasicValue>.SuccessorsImpl successors = node.createSuccessors();
        BranchAnalyzer.Node<? extends BasicValue> successor = successors.current();
        while (successor != null) {
            stack.push(WithFlag.of(successor, false));
            successors.next();
            successor = successors.current();
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
            if (scc != null && isAwait(scc)) {
                label = 2;
            } else if (AsmHelper.isAwait(insnNode)) {
                label = 1;
            } else {
                label = 0;
            }
            boolean visited = withFlag.isFlag();
            if (visited) {
                if (label == 2) {
                    PackageInsnNode newInsnNode = processLoopNode(root);
                    insnNodes[index] = newInsnNode;
                } else if (label == 1) {
                    PackageInsnNode newInsnNode = processAwaitNode(root);
                    insnNodes[index] = newInsnNode;
                } else {
                    insnNodes[index] = insnNode;
                }
            } else {
                if (label == 0) {
                    pushSuccessors(root, stack);
                }
                stack.push(WithFlag.of(root, true));
            }
        }
    }

    private AbstractInsnNode[] collectSuccessors(
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

    private String findLambdaName(BranchAnalyzer.Node<? extends BasicValue> node, Arguments arguments) {
        int mappedIndex = mapped(node.getIndex());
        String desc = Type.getMethodDescriptor(Constants.JPROMISE_DESC, arguments.argTypes(0));
        return classContext.findLambdaByHead(getRootMethodContext(), desc, mappedIndex);
    }

    private MethodNode createLambdaNode(Arguments arguments) {
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

    private void calcExtraAwaitArgumentsType(int validLocals, BranchAnalyzer.Node<? extends BasicValue> node, Arguments arguments) {
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
        // await type -> arguments
        arguments.addArgument(Constants.OBJECT_DESC);
        // x, y, z, a, b, await type
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
        Arguments arguments = calcAwaitArgumentsType(validLocals, node);
        String lambdaName = findLambdaName(node, arguments);
        if (lambdaName == null) {
            MethodNode lambdaNode = createLambdaNode(arguments);
            AsmHelper.updateStack(lambdaNode, getMv().maxStack);
            AsmHelper.updateLocal(lambdaNode, getMv().maxLocals);
            LabelMap labelMap = new LabelMap();
            AbstractInsnNode[] successors = collectSuccessors(node, (in, n) -> in.clone(labelMap));
            buildLambda(lambdaNode, arguments, successors, validLocals, node, null);
            lambdaName = lambdaNode.name;
        }
        insnList.add(LambdaUtils.invokeJAsyncPromiseFunction0(
                classType(),
                lambdaName,
                Constants.OBJECT_DESC,
                isStatic(),
                arguments.argTypes(1)
        ));
        insnList.add(new MethodInsnNode(
                Opcodes.INVOKEINTERFACE,
                Constants.JPROMISE_NAME,
                Constants.JPROMISE_THEN_NAME,
                Constants.JPROMISE_THEN1_DESC.getDescriptor())
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

    private void pop(MethodNode lambdaNode) {
        lambdaNode.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                Constants.JSTACK_NAME,
                Constants.JSTACK_POP_NAME,
                Constants.JSTACK_POP_DESC.getDescriptor(),
                true
        );
    }

    private void primitiveToObject(List<AbstractInsnNode> insnNodes, Type type) {
        // stack: ..., pusher, int
        if (type.getSort() == Type.INT) {
            // Integer.valueOf(top)
            // stack: ..., pusher, int -> ..., pusher, Integer
            insnNodes.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    Constants.INTEGER_NAME,
                    Constants.INTEGER_VALUE_OF_NAME,
                    Constants.INTEGER_VALUE_OF_DESC.getDescriptor(),
                    false
            ));
        }
        // stack: ..., pusher, float
        else if (type.getSort() == Type.FLOAT) {
            // Float.valueOf(top)
            // stack: ..., pusher, float -> ..., pusher, Float
            insnNodes.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    Constants.FLOAT_NAME,
                    Constants.FLOAT_VALUE_OF_NAME,
                    Constants.FLOAT_VALUE_OF_DESC.getDescriptor(),
                    false
            ));
        }
        // stack: ..., pusher, long
        else if (type.getSort() == Type.LONG) {
            // Long.valueOf(top)
            // stack: ..., pusher, long -> ..., pusher, Long
            insnNodes.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    Constants.LONG_NAME,
                    Constants.LONG_VALUE_OF_NAME,
                    Constants.LONG_VALUE_OF_DESC.getDescriptor(),
                    false
            ));
        }
        // stack: ..., pusher, double
        else if (type.getSort() == Type.DOUBLE) {
            // Double.valueOf(top)
            // stack: ..., pusher, double -> ..., pusher, Double
            insnNodes.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    Constants.DOUBLE_NAME,
                    Constants.DOUBLE_VALUE_OF_NAME,
                    Constants.DOUBLE_VALUE_OF_DESC.getDescriptor(),
                    false
            ));
        }
        // stack: ..., pusher, boolean
        else if (type.getSort() == Type.BOOLEAN) {
            // Boolean.valueOf(top)
            // stack: ..., pusher, boolean -> ..., pusher, Boolean
            insnNodes.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    Constants.BOOLEAN_NAME,
                    Constants.BOOLEAN_VALUE_OF_NAME,
                    Constants.BOOLEAN_VALUE_OF_DESC.getDescriptor(),
                    false
            ));
        }
        // stack: ..., pusher, short
        else if (type.getSort() == Type.SHORT) {
            // Short.valueOf(top)
            // stack: ..., pusher, short -> ..., pusher, Short
            insnNodes.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    Constants.SHORT_NAME,
                    Constants.SHORT_VALUE_OF_NAME,
                    Constants.SHORT_VALUE_OF_DESC.getDescriptor(),
                    false
            ));
        }
        // stack: ..., pusher, char
        else if (type.getSort() == Type.CHAR) {
            // Character.valueOf(top)
            // stack: ..., pusher, char -> ..., pusher, Character
            insnNodes.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    Constants.CHARACTER_NAME,
                    Constants.CHARACTER_VALUE_OF_NAME,
                    Constants.CHARACTER_VALUE_OF_DESC.getDescriptor(),
                    false
            ));
        }
        // stack: ..., pusher, byte
        else if (type.getSort() == Type.BYTE) {
            // Byte.valueOf(top)
            // stack: ..., pusher, byte -> ..., pusher, Byte
            insnNodes.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    Constants.BYTE_NAME,
                    Constants.BYTE_VALUE_OF_NAME,
                    Constants.BYTE_VALUE_OF_DESC.getDescriptor(),
                    false
            ));
        }
    }

    private int objectToPrimitive(MethodNode methodNode, Type type) {
        // stack: ..., Integer
        if (type.getSort() == Type.INT) {
            // t.intValue()
            // stack: ..., Integer -> ..., int
            methodNode.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    Constants.INTEGER_NAME,
                    Constants.INTEGER_INT_VALUE_NAME,
                    Constants.INTEGER_INT_VALUE_DESC.getDescriptor(),
                    false
            );
            return 1;
        }
        // stack: ..., Float
        else if (type.getSort() == Type.FLOAT) {
            // t.floatValue()
            // stack: ..., Float -> ..., float
            methodNode.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    Constants.FLOAT_NAME,
                    Constants.FLOAT_FLOAT_VALUE_NAME,
                    Constants.FLOAT_FLOAT_VALUE_DESC.getDescriptor(),
                    false
            );
            return 1;
        }
        // stack: ..., Long
        else if (type.getSort() == Type.LONG) {
            // t.longValue()
            // stack: ..., Long -> ..., long
            methodNode.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    Constants.LONG_NAME,
                    Constants.LONG_LONG_VALUE_NAME,
                    Constants.LONG_LONG_VALUE_DESC.getDescriptor(),
                    false
            );
            return 1;
        }
        // stack: ..., Double
        else if (type.getSort() == Type.DOUBLE) {
            // t.doubleValue()
            // stack: ..., Double -> ..., double
            methodNode.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    Constants.DOUBLE_NAME,
                    Constants.DOUBLE_DOUBLE_VALUE_NAME,
                    Constants.DOUBLE_DOUBLE_VALUE_DESC.getDescriptor(),
                    false
            );
            return 1;
        }
        // stack: ..., Boolean
        else if (type.getSort() == Type.BOOLEAN) {
            // t.booleanValue()
            // stack: ..., Boolean -> ..., boolean
            methodNode.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    Constants.BOOLEAN_NAME,
                    Constants.BOOLEAN_BOOLEAN_VALUE_NAME,
                    Constants.BOOLEAN_BOOLEAN_VALUE_DESC.getDescriptor(),
                    false
            );
            return 1;
        }
        // stack: ..., Short
        else if (type.getSort() == Type.SHORT) {
            // t.shortValue()
            // stack: ..., Short -> ..., short
            methodNode.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    Constants.SHORT_NAME,
                    Constants.SHORT_SHORT_VALUE_NAME,
                    Constants.SHORT_SHORT_VALUE_DESC.getDescriptor(),
                    false
            );
            return 1;
        }
        // stack: ..., Character
        else if (type.getSort() == Type.CHAR) {
            // t.charValue()
            // stack: ..., Character -> ..., char
            methodNode.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    Constants.CHARACTER_NAME,
                    Constants.CHARACTER_CHAR_VALUE_NAME,
                    Constants.CHARACTER_CHAR_VALUE_DESC.getDescriptor(),
                    false
            );
            return 1;
        }
        // stack: ..., Byte
        else if (type.getSort() == Type.BYTE) {
            // t.byteValue()
            // stack: ..., Byte -> ..., byte
            methodNode.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    Constants.BYTE_NAME,
                    Constants.BYTE_BYTE_VALUE_NAME,
                    Constants.BYTE_BYTE_VALUE_DESC.getDescriptor(),
                    false
            );
            return 1;
        }
        return 0;
    }

    private int objectToType(MethodNode methodNode, Type type) {
        if (AsmHelper.needCastTo(Constants.OBJECT_DESC, type)) {
            methodNode.visitTypeInsn(Opcodes.CHECKCAST, type.getInternalName());
            return 1;
        } else {
            return 0;
        }
    }

    private PackageInsnNode pushStack(MethodNode methodNode, boolean loop, BranchAnalyzer.Node<? extends BasicValue> node, AbstractInsnNode[] insnArray) {
        PackageInsnNode packageInsnNode = new PackageInsnNode();
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
                    // The last local var in loop method is stack, it need not be push.
                    if (loop) {
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
        return packageInsnNode;
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

    private int popStack(BranchAnalyzer.Node<? extends BasicValue> node, List<Integer> lambdaMap, boolean loop, MethodNode lambdaNode) {
        int mappedIndex = mapped(node.getIndex());
        // local: this?, JPortal, JStack
        int locals = node.getLocals();
        // The last local var is stack in the loop method.
        int usedLocals = loop ? locals - 1 : locals;
        int portalSlot = isStatic() ? 0 : 1;
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
        for (int i = isStatic() ? 0 : 1; i < locals;) {
            int nextPos;
            BasicValue value = node.getLocal(i);
            if (value != null && value.getType() != null) {
                Type type = value.getType();
                nextPos = i + type.getSize();
            } else {
                nextPos = i + 1;
            }
            // The last local var in loop method is stack, it is not pushed, so can not be popped.
            if (nextPos >= locals && loop) {
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
                            // stack: ..., new type -> ..., new type, new type
                            lambdaNode.visitInsn(Opcodes.DUP);
                            lambdaMap.add(mappedIndex);
                            // stack: ..., new type, new type -> ..., new type, new type, JStack
                            lambdaNode.visitVarInsn(Opcodes.ALOAD, stackSlot);
                            lambdaMap.add(mappedIndex);
                        } else {
                            throw new IllegalStateException("An uninitialized this object is in the wrong position.");
                        }
                    } else {
                        // stack: ..., JStack -> ...,
                        lambdaNode.visitInsn(Opcodes.POP);
                        lambdaMap.add(mappedIndex);
                        // stack: ..., -> ..., new type
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
                // stack: ..., new type -> ..., new type, JStack
                lambdaNode.visitVarInsn(Opcodes.ALOAD, stackSlot);
                lambdaMap.add(mappedIndex);
                System.out.println("An uninitialized this object lost.");
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
            // stack: ..., new type -> ..., new type, JStack
            lambdaNode.visitVarInsn(Opcodes.ALOAD, stackSlot);
            lambdaMap.add(mappedIndex);
            System.out.println("An uninitialized this object lost.");
        }
        // stack: ..., JStack -> ...
        lambdaNode.visitInsn(Opcodes.POP);
        lambdaMap.add(mappedIndex);
        AsmHelper.updateStack(lambdaNode, 1 + stackSize);
        return portalSlot;
    }

    private PackageInsnNode processLoopNode(BranchAnalyzer.Node<? extends BasicValue> node) {
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
        addLambdaContext(outLambda, outLambdaMap, false, true);

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
        String innerLambdaName = findLambdaName(node, arguments);
        if (innerLambdaName == null) {
            AbstractInsnNode insnNode = getMv().instructions.get(node.getIndex());
            if (!(insnNode instanceof LabelNode)) {
                // 因为这个指令是至少2个指令的后继，只有 LabelNode 可以是多个指令的后继
                throw new IllegalStateException("This is impossible!");
            }
            LabelNode labelNode = (LabelNode) insnNode;
            // (stack) -> ...
            MethodNode innerLambda = createLambdaNode(arguments);
            LabelNode portalLabel = new LabelNode();
            LabelMap labelMap = new LabelMap();
            labelMap.put(labelNode, portalLabel);
            AbstractInsnNode[] successors = collectSuccessors(node, (in, n) -> in.clone(labelMap));
            buildLambda(innerLambda, null, successors, -1, node, portalLabel);
            innerLambdaName = innerLambda.name;
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
        addLambdaContext(midLambda, midLambdaMap, false, true);
        PackageInsnNode packageInsnNode = pushStack(getMv(), loop, node, insnNodes);
        pushThenImmediate(getMv(), packageInsnNode, outLambda);
        packageInsnNode.complete();
        return packageInsnNode;
    }

    private void buildLambda(MethodNode lambdaNode, Arguments arguments, AbstractInsnNode[] insnArray, int locals, BranchAnalyzer.Node<? extends BasicValue> node, LabelNode portalLabel) {
        List<Integer> lambdaMap = new ArrayList<>();
        int index = node.getIndex();
        int mappedIndex = mapped(index);
        lambdaNode.visitCode();
        LabelNode startLabelNode = new LabelNode();
        lambdaNode.instructions.add(startLabelNode);
        lambdaMap.add(mappedIndex);
        List<LocalVariableNode> localVariableNodes = new ArrayList<>();
        LocalVariableNode[] localVariableArray = new LocalVariableNode[getMv().maxLocals];
        updateLocalVar(localVariableArray, localVariableNodes, startLabelNode, node);
        boolean isStatic = isStatic();
        int offset = isStatic ? 0 : 1;
        int portalSlot = -1;

        if (portalLabel == null) {
            // arguments: x, y, z, a, b, await type -> stack: a, b, await result
            // locals: this?, x, y, z, a, b, await type
            int stacks = node.getStackSize();
            int j = offset;
            for (Arguments.ExtendType extendType : arguments.getTypes()) {
                // arguments 的构成恰好同构于 locals + stack，
                // 虽然最后一位, arguments 中是 await 的结果，而 frame 中是 Promise. 但它们都是 Object, 可以做相同处理.
                if (j >= locals) {
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
            AsmHelper.updateStack(lambdaNode, stacks);
        } else {
            portalSlot = popStack(node, lambdaMap, loop, lambdaNode);
        }
        if (lambdaNode.instructions.size() > 1) {
            LabelNode restoreLabelNode = new LabelNode();
            lambdaNode.instructions.add(restoreLabelNode);
            lambdaMap.add(mappedIndex);
        }

        LabelNode jumpEndNode = null;
        List<AbstractInsnNode> jumpNodes = null;
        if (portalLabel != null) {
            jumpNodes = new ArrayList<>();
            jumpNodes.add(portalLabel);
            PackageInsnNode packageInsnNode = pushStack(lambdaNode, loop, node, insnArray);
            jumpNodes.addAll(packageInsnNode.getInsnNodes());
            // push portal to stack
            jumpNodes.add(new VarInsnNode(Opcodes.ALOAD, portalSlot));
            AsmHelper.appendStack(lambdaNode, node, 1);
            // push jump lambda
            InvokeDynamicInsnNode jumpLambdaInsnNode = LambdaUtils.invokePortalJump();
            jumpNodes.add(jumpLambdaInsnNode);
            // thenImmediate(jumpLambda)
            jumpNodes.add(new MethodInsnNode(
                    Opcodes.INVOKEINTERFACE,
                    Constants.JPROMISE_NAME,
                    Constants.JPROMISE_THEN_IMMEDIATE_NAME,
                    Constants.JPROMISE_THEN_IMMEDIATE0_DESC.getDescriptor(),
                    true
            ));
            jumpNodes.add(new InsnNode(Opcodes.ARETURN));
            jumpEndNode = new LabelNode();
            jumpNodes.add(jumpEndNode);
        }

        LinkedList<AbstractInsnNode> insnList = new LinkedList<>();
        List<Integer> insnListMap = new ArrayList<>();
        List<AbstractInsnNode> preInsnList = new LinkedList<>();
        List<Integer> preInsnListMap = new ArrayList<>();
        List<BranchAnalyzer.Node<? extends BasicValue>> preFrames = new LinkedList<>();
        int i = 0;
        boolean reconnect = false;
        for (AbstractInsnNode insnNode : insnArray) {
            if (insnNode != null && i != index) {
                List<AbstractInsnNode> target;
                List<Integer> targetMap;
                if (i < index) {
                    target = preInsnList;
                    targetMap = preInsnListMap;
                    if (i == index - 1) {
                        BranchAnalyzer.Node<BasicValue> frame = getFrames()[i];
                        if (frame.getSuccessors().contains(node)) {
                            reconnect = true;
                        }
                    }
                } else {
                    target = insnList;
                    targetMap = insnListMap;
                }
                BranchAnalyzer.Node<BasicValue> frame = getFrames()[i];
                int originalIndex = mapped(i);
                if (insnNode instanceof PackageInsnNode) {
                    PackageInsnNode packageInsnNode = (PackageInsnNode) insnNode;
                    for (AbstractInsnNode n : packageInsnNode.getInsnNodes()) {
                        target.add(n);
                        targetMap.add(originalIndex);
                        if (target == insnList) {
                            updateLocalVar(localVariableArray, localVariableNodes, n, frame);
                        } else {
                            preFrames.add(frame);
                        }
                    }
                } else {
                    target.add(insnNode);
                    targetMap.add(originalIndex);
                    if (target == insnList) {
                        updateLocalVar(localVariableArray, localVariableNodes, insnNode, frame);
                    } else {
                        preFrames.add(frame);
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
            insnList.add(preInsn);
            insnListMap.add(preInsnMap);
            updateLocalVar(localVariableArray, localVariableNodes, preInsn, preFrame);
        }
        if (reconnect) {
            insnList.add(new JumpInsnNode(Opcodes.GOTO, reconnectLabel));
            insnListMap.add(mappedIndex);
        }
        LabelNode endLabel = new LabelNode();
        if (insnList.getLast() instanceof LabelNode) {
            endLabel = (LabelNode) insnList.getLast();
            completeLocalVar(localVariableArray, localVariableNodes, null, false);
        } else {
            insnList.add(endLabel);
            insnListMap.add(mappedIndex);
            completeLocalVar(localVariableArray, localVariableNodes, endLabel, false);
        }
        if (jumpEndNode != null) {
            endLabel = jumpEndNode;
        }

        Iterator<AbstractInsnNode> insnIter = insnList.iterator();
        Iterator<Integer> insnMapIter = insnListMap.iterator();
        while (insnIter.hasNext()) {
            lambdaNode.instructions.add(insnIter.next());
            lambdaMap.add(insnMapIter.next());
        }
        if (jumpNodes != null) {
            for (AbstractInsnNode jumpNode : jumpNodes) {
                lambdaNode.instructions.add(jumpNode);
            }
            addManyMap(lambdaMap, mappedIndex, jumpNodes.size());
        }
        LocalVariableNode thisVarNode = findThisVar(localVariableArray, isStatic);
        if (thisVarNode != null && thisVarNode.start != null) {
            thisVarNode.end = endLabel;
            localVariableNodes.add(thisVarNode);
        }
        lambdaNode.localVariables = localVariableNodes;

        addLambdaContext(lambdaNode, lambdaMap, portalLabel != null, false);
    }

    private int mapped(int index) {
        return map != null ? map.get(index) : index;
    }

    private static void addManyMap(List<Integer> map, int value, int num) {
        for (int i = 0; i < num; ++i) {
            map.add(value);
        }
    }
}
