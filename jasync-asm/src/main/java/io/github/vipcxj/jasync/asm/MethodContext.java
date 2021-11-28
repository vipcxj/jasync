package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.io.PrintWriter;
import java.util.*;
import java.util.function.BiFunction;

public class MethodContext {
    private final ClassContext classContext;
    private final MethodNode mv;
    private final JAsyncInfo info;
    private final BranchAnalyzer.Node<BasicValue>[] frames;
    private final Map<LabelNode, LabelNode> cloneLabels;
    private final List<Integer> localsToUpdate;
    private final List<Integer> stacksToUpdate;
    private final boolean loop;
    private int index = 0;

    protected final MethodContext parent;
    private final List<MethodContext> children;
    private final AbstractInsnNode[] insnNodes;
    private List<Set<Integer>> loops;

    public MethodContext(ClassContext classContext, MethodNode mv, boolean loop, MethodContext parent) {
        this.classContext = classContext;
        this.mv = mv;
        this.info = JAsyncInfo.of(mv);
        this.localsToUpdate = new ArrayList<>();
        this.stacksToUpdate = new ArrayList<>();
        this.cloneLabels = new HashMap<>();
        AsmHelper.collectLabels(mv, cloneLabels);
        this.loop = loop;
        BranchAnalyzer analyzer = new BranchAnalyzer();
        try {
            analyzer.analyzeAndComputeMaxs(classContext.getName(), mv);
            this.frames = analyzer.getNodes();
        } catch (AnalyzerException e) {
            throw new RuntimeException(e);
        }
        this.parent = parent;
        this.children = new ArrayList<>();
        insnNodes = new AbstractInsnNode[this.frames.length];
    }

    public MethodContext createChild(MethodNode methodNode, boolean loop) {
        return new MethodContext(classContext, methodNode, loop, this);
    }

    public MethodNode getMv() {
        return mv;
    }

    public JAsyncInfo getInfo() {
        return getRootMethodContext().info;
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

    public <T extends AbstractInsnNode> T cloneInsn(T node) {
        //noinspection unchecked
        return (T) node.clone(cloneLabels);
    }

    public void replaceLabel(LabelNode from, LabelNode to) {
        cloneLabels.put(from, to);
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

    public void addLambdaContext(MethodNode lambdaNode, boolean loop) {
        MethodContext childContext = createChild(lambdaNode, loop);
        children.add(childContext);
        this.classContext.addLambda(getRootMethodContext(), childContext);
    }

    public void addLambdaContext(MethodNode lambdaNode) {
        addLambdaContext(lambdaNode, false);
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
            for (int i = 0; i < insnNodes.length; ++i) {
                AbstractInsnNode insnNode = insnNodes[i];
                if (insnNode != null) {
                    BranchAnalyzer.Node<BasicValue> frame = frames[i];
                    Type needCastTo = frame.getNeedCastTo();
                    if (needCastTo != null) {
                        newInsnList.add(new TypeInsnNode(Opcodes.CHECKCAST, needCastTo.getInternalName()));
                    }
                    if (insnNode instanceof PackageInsnNode) {
                        PackageInsnNode packageInsnNode = (PackageInsnNode) insnNode;
                        for (AbstractInsnNode node : packageInsnNode.getInsnNodes()) {
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
        int base = (!isStatic() && includeThis) ? 1 : 0;
        for (int i = base; i < localVariableArray.length; ++i) {
            pushLocalVariable(localVariableArray, i, localVariableList, endNode);
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

    private MethodNode createLambdaNode(List<Type> arguments) {
        int access = Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC;
        if (isStatic()) {
            access |= Opcodes.ACC_STATIC;
        }
        int locals = 0;
        for (Type argument : arguments) {
            locals += argument.getSize();
        }
        MethodNode methodNode = new MethodNode(
                Constants.ASM_VERSION,
                access,
                nextLambdaName(),
                Type.getMethodDescriptor(Constants.JPROMISE_DESC, arguments.toArray(new Type[0])),
                null,
                new String[] { Constants.THROWABLE_NAME }
        );
        methodNode.maxStack = 0;
        methodNode.maxLocals = isStatic() ? locals : (locals + 1);
        return methodNode;
    }

    private void calcExtraAwaitArgumentsType(int validLocals, BranchAnalyzer.Node<? extends BasicValue> node, List<Type> arguments) {
        // locals: this?, x, y, z.
        // x, y, z -> arguments
        int start = isStatic() ? 0 : 1;
        for (int i = start; i < validLocals;) {
            BasicValue value = node.getLocal(i);
            Type type = value.getType();
            if (type != null) {
                arguments.add(type);
                i += type.getSize();
            } else {
                arguments.add(Constants.OBJECT_DESC);
                ++i;
            }
        }
        int stackSize = node.getStackSize();
        int iMax = stackSize - 1;
        // stack: a, b -> arguments
        for (int i = 0; i < iMax; ++i) {
            BasicValue value = node.getStack(i);
            Type type = value.getType();
            if (type != null) {
                arguments.add(type);
            } else {
                arguments.add(Constants.OBJECT_DESC);
            }
        }
    }

    private List<Type> calcAwaitArgumentsType(int validLocals, BranchAnalyzer.Node<? extends BasicValue> frame) {
        // stack: a, b, promise | locals: this?, x, y, z
        List<Type> arguments = new ArrayList<>();
        calcExtraAwaitArgumentsType(validLocals, frame, arguments);
        // await type -> arguments
        arguments.add(Constants.OBJECT_DESC);
        // x, y, z, a, b, await type
        return arguments;
    }

    private void restoreStack(List<AbstractInsnNode> insnNodes, BranchAnalyzer.Node<? extends BasicValue> node, int maxLocals, int num) {
        for (int i = 0, iLocal = maxLocals; i < num; ++i) {
            BasicValue value = node.getStack(i);
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
        List<AbstractInsnNode> insnNodes = packageInsnNode.getInsnNodes();
        int locals = node.getLocals();
        int stackSize = node.getStackSize();
        // stack: promise | locals: this?, x, y, z
        if (stackSize == 1) {
            if (!isStatic()) {
                // load this to stack
                // stack: promise | locals: this, x, y, z -> stack: promise, this | locals: this, x, y, z
                insnNodes.add(new VarInsnNode(Opcodes.ALOAD, 0));
            }
            // push the previous locals to the stack except this
            // stack: promise, this? | locals: this?, x, y, z -> stack: promise, this?, x, y, z | locals: this?, x, y, z
            AsmHelper.pushLocalToStack(locals, isStatic(), node, insnNodes);
        }
        // stack: a, b, promise | locals: this?, x, y, z
        else {
            // store the current stack to the locals (offset by locals). the first one (index of locals) should be the promise
            // stack: a, b, promise | locals: this?, x, y, z -> stack: [] | locals: this?, x, y, z, promise, b, a
            int maxLocals = AsmHelper.storeStackToLocal(locals, node, insnNodes);
            updateLocals(maxLocals);
            // push the target promise to stack
            // stack: [] | locals: this?, x, y, z, promise, b, a -> stack: promise | locals: this?, x, y, z, promise, b, a
            insnNodes.add(new VarInsnNode(Opcodes.ALOAD, locals));
            if (!isStatic()) {
                // load this to stack
                // stack: promise | locals: this, x, y, z, promise, b, a -> stack: promise, this | locals: this, x, y, z, promise, b, a
                insnNodes.add(new VarInsnNode(Opcodes.ALOAD, 0));
            }
            // push the previous locals to the stack except this
            // stack: promise, this? | locals: this?, x, y, z, promise, b, a -> stack: promise, this?, x, y, z | locals: this?, x, y, z, promise, b, a
            AsmHelper.pushLocalToStack(locals, isStatic(), node, insnNodes);
            // push the previous stack from locals to the stack, except the previous stack top, which is the promise.
            // stack: promise, this?, x, y, z | locals: this?, x, y, z, promise, b, a -> stack: promise, this?, x, y, z, a, b | locals: this?, x, y, z, promise, b, a
            restoreStack(insnNodes, node, maxLocals, stackSize - 1);
        }
        updateStacks(stackSize + locals);
        List<Type> arguments = calcAwaitArgumentsType(locals, node);
        MethodNode lambdaNode = createLambdaNode(arguments);
        AsmHelper.updateStack(lambdaNode, getMv().maxStack);
        AsmHelper.updateLocal(lambdaNode, getMv().maxLocals);
        insnNodes.add(LambdaUtils.invokeJAsyncPromiseFunction0(
                classType(),
                lambdaNode.name,
                Constants.OBJECT_DESC,
                isStatic(),
                arguments.subList(0, arguments.size() - 1).toArray(new Type[arguments.size() - 1])
        ));
        insnNodes.add(new MethodInsnNode(
                Opcodes.INVOKEINTERFACE,
                Constants.JPROMISE_NAME,
                Constants.JPROMISE_THEN_NAME,
                Constants.JPROMISE_THEN1_DESC.getDescriptor())
        );
        insnNodes.add(new InsnNode(Opcodes.ARETURN));
        packageInsnNode.complete();
        AbstractInsnNode[] successors = collectSuccessors(node, (in, n) -> cloneInsn(in));
        buildLambda(lambdaNode, arguments, successors, locals, node, null);
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

    private void objectToPrimitive(MethodNode methodNode, Type type) {
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
        }
    }

    private PackageInsnNode pushStack(MethodNode methodNode, boolean loop, BranchAnalyzer.Node<? extends BasicValue> node, MethodNode lambdaNode) {
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
        return packageInsnNode;
    }

    private int popStack(BranchAnalyzer.Node<? extends BasicValue> node, boolean loop, MethodNode lambdaNode) {
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
            AsmHelper.updateStack(lambdaNode, 1);
            portalSlot += usedLocals;
            // stack: JPortal -> []
            // locals: ..., -> ..., JPortal
            lambdaNode.visitVarInsn(Opcodes.ASTORE, portalSlot);
            AsmHelper.updateLocal(lambdaNode, portalSlot + 1);

            // load stack to index: 1 / 2 + usedLocals
            // stack: [] -> JStack
            lambdaNode.visitVarInsn(Opcodes.ALOAD, stackSlot);
            stackSlot = portalSlot + 1;
            // stack: JStack -> []
            // locals: ..., JPortal -> ..., JPortal, JStack
            lambdaNode.visitVarInsn(Opcodes.ASTORE, stackSlot);
            AsmHelper.updateLocal(lambdaNode, stackSlot + 1);
        }
        // push stack
        // stack: [] -> JStack
        lambdaNode.visitVarInsn(Opcodes.ALOAD, stackSlot);
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
            // stack: JStack, JStack -> JStack, T
            pop(lambdaNode);
            AsmHelper.updateStack(lambdaNode, 2);
            if (value != null && value.getType() != null) {
                Type type = value.getType();
                if (type.getSort() != Type.OBJECT && type.getSort() != Type.ARRAY) {
                    // stack: JStack, T -> JStack, t
                    objectToPrimitive(lambdaNode, type);
                }
                // stack: JStack, t -> JStack
                // local: ..., u, ... -> ..., t, ...
                lambdaNode.visitVarInsn(type.getOpcode(Opcodes.ISTORE), i);
            } else {
                // stack: JStack, t -> JStack
                // local: ..., u, ... -> ..., t, ...
                lambdaNode.visitVarInsn(Opcodes.ASTORE, i);
            }
            i = nextPos;
        }
        // restore stacks
        int stackSize = node.getStackSize();
        for (int i = 0; i < stackSize; ++i) {
            // stack: ..., JStack -> ..., T
            pop(lambdaNode);
            BasicValue value = node.getStack(i);
            if (value != null && value.getType() != null) {
                Type type = value.getType();
                if (type.getSort() != Type.OBJECT && type.getSort() != Type.ARRAY) {
                    // stack: ..., T -> ..., t
                    objectToPrimitive(lambdaNode, type);
                }
            }
            // stack: ..., t -> ..., t, JStack
            lambdaNode.visitVarInsn(Opcodes.ALOAD, stackSlot);
        }
        // stack: ..., JStack -> ...
        lambdaNode.visitInsn(Opcodes.POP);
        AsmHelper.updateStack(lambdaNode, 1 + stackSize);
        return portalSlot;
    }

    private PackageInsnNode processLoopNode(BranchAnalyzer.Node<? extends BasicValue> node) {
        // () -> JPromise.portal(midLambda)
        MethodNode outLambda = createLambdaNode(Collections.emptyList());
        // (portal) -> JContext.popStack(innerLambda)
        MethodNode midLambda = createLambdaNode(Collections.singletonList(Constants.JPORTAL_DESC));
        // (stack) -> ...
        MethodNode innerLambda = createLambdaNode(Arrays.asList(Constants.JPORTAL_DESC, Constants.JSTACK_DESC));

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
        addLambdaContext(outLambda);

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
        midLambda.instructions.add(LambdaUtils.invokeJAsyncPromiseFunction0(
                classType(),
                innerLambda.name,
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
        addLambdaContext(midLambda);

        AbstractInsnNode insnNode = getMv().instructions.get(node.getIndex());
        if (!(insnNode instanceof LabelNode)) {
            // 因为这个指令是至少2个指令的后继，只有 LabelNode 可以是多个指令的后继
            throw new IllegalStateException("This is impossible!");
        }
        LabelNode labelNode = (LabelNode) insnNode;
        PackageInsnNode packageInsnNode = pushStack(getMv(), loop, node, outLambda);
        packageInsnNode.complete();
        LabelNode portalLabel = new LabelNode();
        replaceLabel(labelNode, portalLabel);
        List<AbstractInsnNode> jumpInsnNodes = new ArrayList<>();
        List<Frame<? extends BasicValue>> jumpFrames = new ArrayList<>();
        AbstractInsnNode[] successors = collectSuccessors(node, (in, n) -> {
            if (n.getSuccessors().contains(node)) {
                jumpInsnNodes.add(in);
                jumpFrames.add(n);
            }
            return cloneInsn(in);
        });
        PrintWriter pw = new PrintWriter(System.out);
        pw.println("Jump info:");
        InsnTextifier insnTextifier = new InsnTextifier(jumpInsnNodes, jumpFrames);
        insnTextifier.getHelper().print(pw, true);
        pw.println("Target frame");
        pw.print(InsnTextifier.printFrame(node, insnTextifier.getHelper(), insnTextifier.getTab()));
        pw.println("Jump Insn:");
        insnTextifier.print(pw);
        buildLambda(innerLambda, null, successors, -1, node, portalLabel);
        return packageInsnNode;
    }

    private void buildLambda(MethodNode lambdaNode, List<Type> arguments, AbstractInsnNode[] insnArray, int locals, BranchAnalyzer.Node<? extends BasicValue> node, LabelNode portalLabel) {
        lambdaNode.visitCode();
        LabelNode startLabelNode = new LabelNode();
        lambdaNode.instructions.add(startLabelNode);
        LabelNode restoreLabelNode = new LabelNode();
        boolean isStatic = isStatic();
        int offset = isStatic ? 0 : 1;
        int portalSlot = -1;

        List<LocalVariableNode> localVariableNodes = new ArrayList<>();
        LocalVariableNode[] localVariableArray = new LocalVariableNode[getMv().maxLocals];
        if (portalLabel == null) {
            // arguments: x, y, z, a, b, await type -> stack: a, b, await result
            // locals: this?, x, y, z, a, b, await type
            int stacks = node.getStackSize();
            AbstractInsnNode[] insnNodes = new AbstractInsnNode[stacks];
            int j = offset, k = 0;
            for (Type type : arguments) {
                if (j >= locals && j < locals + stacks) {
                    if (type != null) {
                        insnNodes[k++] = new VarInsnNode(type.getOpcode(Opcodes.ILOAD), j);
                    } else {
                        insnNodes[k++] = new InsnNode(Opcodes.ACONST_NULL);
                    }
                }
                if (type != null) {
                    j += type.getSize();
                } else {
                    ++j;
                }
            }
            for (AbstractInsnNode insnNode : insnNodes) {
                lambdaNode.instructions.add(insnNode);
            }
            AsmHelper.updateStack(lambdaNode, stacks);
        } else {
            portalSlot = popStack(node, loop, lambdaNode);
        }
        if (lambdaNode.instructions.size() == 1) {
            restoreLabelNode = startLabelNode;
        }
        updateLocalVar(localVariableArray, localVariableNodes, restoreLabelNode, node);

        LinkedList<AbstractInsnNode> insnList = new LinkedList<>();
        List<AbstractInsnNode> preInsnList = new LinkedList<>();
        List<BranchAnalyzer.Node<? extends BasicValue>> preFrames = new LinkedList<>();
        int index = node.getIndex();
        int i = 0;
        boolean reconnect = false;
        for (AbstractInsnNode insnNode : insnArray) {
            if (insnNode != null && i != index) {
                List<AbstractInsnNode> target;
                if (i < index) {
                    target = preInsnList;
                    if (i == index - 1) {
                        BranchAnalyzer.Node<BasicValue> frame = getFrames()[i];
                        if (frame.getSuccessors().contains(node)) {
                            reconnect = true;
                        }
                    }
                } else {
                    target = insnList;
                }
                BranchAnalyzer.Node<BasicValue> frame = getFrames()[i];
                if (insnNode instanceof PackageInsnNode) {
                    PackageInsnNode packageInsnNode = (PackageInsnNode) insnNode;
                    for (AbstractInsnNode n : packageInsnNode.getInsnNodes()) {
                        target.add(n);
                        if (target == insnList) {
                            updateLocalVar(localVariableArray, localVariableNodes, n, frame);
                        } else {
                            preFrames.add(frame);
                        }
                    }
                } else {
                    target.add(insnNode);
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
        }
        Iterator<AbstractInsnNode> preInsnIter = preInsnList.iterator();
        Iterator<BranchAnalyzer.Node<? extends BasicValue>> preFrameIter = preFrames.iterator();
        while (preInsnIter.hasNext()) {
            AbstractInsnNode preInsn = preInsnIter.next();
            BranchAnalyzer.Node<? extends BasicValue> preFrame = preFrameIter.next();
            insnList.add(preInsn);
            updateLocalVar(localVariableArray, localVariableNodes, preInsn, preFrame);
        }
        if (reconnect) {
            insnList.add(new JumpInsnNode(Opcodes.GOTO, reconnectLabel));
        }
        LabelNode endLabel = new LabelNode();
        if (insnList.getLast() instanceof LabelNode) {
            endLabel = (LabelNode) insnList.getLast();
            completeLocalVar(localVariableArray, localVariableNodes, null, false);
        } else {
            insnList.add(endLabel);
            completeLocalVar(localVariableArray, localVariableNodes, endLabel, false);
        }

        for (AbstractInsnNode insnNode : insnList) {
            lambdaNode.instructions.add(insnNode);
        }

        if (portalLabel != null) {
            lambdaNode.instructions.add(portalLabel);
            // push portal to stack
            lambdaNode.visitVarInsn(Opcodes.ALOAD, portalSlot);
            AsmHelper.appendStack(lambdaNode, node, 1);
            MethodNode jumpLambda = createLambdaNode(Collections.singletonList(Constants.JPORTAL_DESC));
            PackageInsnNode packageInsnNode = pushStack(lambdaNode, loop, node, jumpLambda);
            for (AbstractInsnNode insnNode : packageInsnNode.getInsnNodes()) {
                lambdaNode.instructions.add(insnNode);
            }
            // push jump lambda
            InvokeDynamicInsnNode jumpLambdaInsnNode = LambdaUtils.invokePortalJump();
            lambdaNode.instructions.add(jumpLambdaInsnNode);
            // thenImmediate(jumpLambda)
            lambdaNode.visitMethodInsn(Opcodes.INVOKEINTERFACE, Constants.JPROMISE_NAME, Constants.JPROMISE_THEN_IMMEDIATE_NAME, Constants.JPROMISE_THEN_IMMEDIATE0_DESC.getDescriptor(), true);
            lambdaNode.visitInsn(Opcodes.ARETURN);
            endLabel = new LabelNode();
            lambdaNode.instructions.add(endLabel);
        }
        if (!isStatic && localVariableArray.length > 0) {
            LocalVariableNode thisVarNode = localVariableArray[0];
            if (thisVarNode != null && thisVarNode.start != null) {
                thisVarNode.end = endLabel;
                localVariableNodes.add(thisVarNode);
            }
        }
        lambdaNode.localVariables = localVariableNodes;

        addLambdaContext(lambdaNode, portalLabel != null);
    }

}
