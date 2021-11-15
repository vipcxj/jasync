package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.BasicValue;

import java.util.*;
import java.util.function.BiFunction;

public class CodePiece {
    protected final MethodContext methodContext;
    protected final CodePiece parent;
    private final List<CodePiece> children;
    private final AbstractInsnNode[] insnNodes;
    private List<Set<Integer>> loops;

    protected CodePiece(MethodContext methodContext, CodePiece parent) {
        this.methodContext = methodContext;
        this.parent = parent;
        this.children = new ArrayList<>();
        insnNodes = new AbstractInsnNode[methodContext.getFrames().length];
    }

    public void process() {
        BranchAnalyzer.Node<BasicValue>[] nodes = methodContext.getFrames();
        if (nodes.length > 0) {
            loops = GraphUtils.tarjan(nodes);
            Arrays.fill(insnNodes, null);
            process(nodes[0]);
            InsnList newInsnList = new InsnList();
            for (AbstractInsnNode insnNode : insnNodes) {
                if (insnNode != null) {
                    if (insnNode instanceof PackageInsnNode) {
                        PackageInsnNode packageInsnNode = (PackageInsnNode) insnNode;
                        for (AbstractInsnNode node : packageInsnNode.getInsnNodes()) {
                            newInsnList.add(node);
                        }
                    } else {
                        newInsnList.add(insnNode);
                    }
                }
            }
            methodContext.getMv().instructions = newInsnList;
            methodContext.updateMax();
            for (CodePiece child : children) {
                child.process();
            }
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
            AbstractInsnNode insnNode = methodContext.getMv().instructions.get(index);
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
            AbstractInsnNode insnNode = methodContext.getMv().instructions.get(index);
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
                    AbstractInsnNode newInsnNode = processLoopNode(root);
                    insnNodes[index] = newInsnNode;
                } else if (label == 1) {
                    AbstractInsnNode newInsnNode = processAwaitNode(root);
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

    private InsnList collectSuccessors(
            BranchAnalyzer.Node<? extends BasicValue> root,
            BiFunction<AbstractInsnNode, BranchAnalyzer.Node<? extends BasicValue>, AbstractInsnNode> mapper
    ) {
        AbstractInsnNode[] insnNodes = new AbstractInsnNode[methodContext.getFrames().length];
        Deque<WithFlag<BranchAnalyzer.Node<? extends BasicValue>>> stack = new ArrayDeque<>();
        stack.push(WithFlag.of(root, false));
        while (!stack.isEmpty()) {
            WithFlag<BranchAnalyzer.Node<? extends BasicValue>> withFlag = stack.pop();
            BranchAnalyzer.Node<? extends BasicValue> node = withFlag.getData();
            boolean visited = withFlag.isFlag();
            int index = node.getIndex();
            if (visited) {
                AbstractInsnNode insnNode = methodContext.getMv().instructions.get(index);
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
        InsnList insnList = new InsnList();
        InsnList preInsnList = new InsnList();
        int index = root.getIndex();
        int i = 0;
        boolean reconnect = false;
        for (AbstractInsnNode insnNode : insnNodes) {
            if (insnNode != null && i != index) {
                InsnList target;
                if (i < index) {
                    target = preInsnList;
                    if (i == index - 1) {
                        BranchAnalyzer.Node<BasicValue> frame = methodContext.getFrames()[i];
                        if (frame.getSuccessors().contains(root)) {
                            reconnect = true;
                        }
                    }
                } else {
                    target = insnList;
                }
                if (insnNode instanceof PackageInsnNode) {
                    PackageInsnNode packageInsnNode = (PackageInsnNode) insnNode;
                    for (AbstractInsnNode node : packageInsnNode.getInsnNodes()) {
                        target.add(node);
                    }

                } else {
                    target.add(insnNode);
                }
            }
            ++i;
        }
        LabelNode reconnectLabel = new LabelNode();
        if (reconnect) {
            insnList.insertBefore(insnList.getFirst(), reconnectLabel);
        }
        insnList.add(preInsnList);
        if (reconnect) {
            insnList.add(new JumpInsnNode(Opcodes.GOTO, reconnectLabel));
        }
        return insnList;
    }

    private MethodNode createLambdaNode(List<Type> arguments) {
        int access = Opcodes.ACC_PRIVATE;
        if (methodContext.isStatic()) {
            access |= Opcodes.ACC_STATIC;
        }
        return new MethodNode(
                Constants.ASM_VERSION,
                access,
                methodContext.nextLambdaName(),
                Type.getMethodDescriptor(Constants.JPROMISE_DESC, arguments.toArray(new Type[0])),
                null,
                new String[] { Constants.THROWABLE_NAME }
        );
    }

    private void calcExtraArgumentsType(int validLocals, BranchAnalyzer.Node<? extends BasicValue> node, List<Type> arguments, boolean allStack) {
        // locals: x, y, z -> arguments
        for (int i = 0; i < validLocals;) {
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
        int iMax = allStack ? stackSize : (stackSize - 1);
        // stack: a, b, c or a, b -> arguments
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
        // stack: a, b, promise | locals: x, y, z
        List<Type> arguments = new ArrayList<>();
        calcExtraArgumentsType(validLocals, frame, arguments, false);
        // await type -> arguments
        arguments.add(Constants.OBJECT_DESC);
        // context -> arguments
        arguments.add(Constants.JCONTEXT_DESC);
        // x, y, z, a, b, await type, context
        return arguments;
    }

    private List<Type> calcLoopArgumentsType(int validLocals, BranchAnalyzer.Node<? extends BasicValue> frame) {
        // stack: a, b, c | locals: x, y, z
        List<Type> arguments = new ArrayList<>();
        calcExtraArgumentsType(validLocals, frame, arguments, true);
        // JPortal type -> arguments
        arguments.add(Constants.JPORTAL_DESC);
        arguments.add(Constants.JCONTEXT_DESC);
        // x, y, z, a, b, c, JPortal, JContext
        return arguments;
    }

    private AbstractInsnNode processAwaitNode(BranchAnalyzer.Node<? extends BasicValue> node) {
        PackageInsnNode packageInsnNode = new PackageInsnNode();
        List<AbstractInsnNode> insnNodes = packageInsnNode.getInsnNodes();
        int validLocals = AsmHelper.calcValidLocals(node);
        // stack: promise | locals: x, y, z
        if (node.getStackSize() == 1) {
            // push the previous locals to the stack
            // stack: promise | locals: x, y, z -> stack: promise, x, y, z | locals: x, y, z
            AsmHelper.pushLocalToStack(validLocals, node, insnNodes);
            methodContext.updateStacks(node.getStackSize() + validLocals);
        }
        // stack: a, b, promise | locals: x, y, z
        else {
            // store the current stack to the locals (offset by locals). the first one (index of locals) should be the promise
            // stack: a, b, promise | locals: x, y, z -> stack: [] | locals: x, y, z, promise, b, a
            int maxLocals = AsmHelper.storeStackToLocal(validLocals, node, insnNodes);
            methodContext.updateLocals(maxLocals);
            // push the target promise to stack
            // stack: [] | locals: x, y, z, promise, b, a -> stack: promise | locals: x, y, z, promise, b, a
            insnNodes.add(new VarInsnNode(Opcodes.ALOAD, validLocals));
            // push the previous locals to the stack
            // stack: promise | locals: x, y, z, promise, b, a -> stack: promise, x, y, z | locals: x, y, z, promise, b, a
            AsmHelper.pushLocalToStack(validLocals, node, insnNodes);
            // push the previous stack from locals to the stack, except the previous stack top, which is the promise.
            // stack: promise, x, y, z | locals: x, y, z, promise, b, a -> stack: promise, x, y, z, a, b | locals: x, y, z, promise, b, a
            int stackSize = node.getStackSize();
            for (int i = 0, iLocal = maxLocals; i < stackSize - 1; ++i) {
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
            methodContext.updateStacks(stackSize + validLocals);
        }
        List<Type> arguments = calcAwaitArgumentsType(validLocals, node);
        MethodNode lambdaNode = createLambdaNode(arguments);
        insnNodes.add(LambdaUtils.invokeJAsyncPromiseFunction0(
                methodContext.classType(),
                lambdaNode.name,
                methodContext.isStatic(),
                arguments.subList(0, arguments.size() - 2).toArray(new Type[arguments.size() - 2])
        ));
        insnNodes.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                Constants.JPROMISE_NAME,
                Constants.JPROMISE_THEN_WITH_CONTEXT_NAME,
                Constants.JPROMISE_THEN_WITH_CONTEXT_DESC.getDescriptor())
        );
        insnNodes.add(new InsnNode(Opcodes.ARETURN));
        InsnList successors = collectSuccessors(node, (in, n) -> methodContext.cloneInsn(in));
        buildLambda(lambdaNode, arguments, successors, validLocals, node.getStackSize(), -1, null);
        return packageInsnNode;
    }

    private AbstractInsnNode processLoopNode(BranchAnalyzer.Node<? extends BasicValue> node) {
        AbstractInsnNode insnNode = methodContext.getMv().instructions.get(node.getIndex());
        if (!(insnNode instanceof LabelNode)) {
            // 因为这个指令是至少2个指令的后继，只有 LabelNode 可以是多个指令的后继
            throw new IllegalStateException("This is impossible!");
        }
        LabelNode labelNode = (LabelNode) insnNode;
        PackageInsnNode packageInsnNode = new PackageInsnNode();
        List<AbstractInsnNode> insnNodes = packageInsnNode.getInsnNodes();
        // 因为是 LabelNode，对 frame 不做任何改变，所以执行后 frame 就是 执行前 frame
        int validLocals = AsmHelper.calcValidLocals(node);
        // stack: a, b, c | locals: x, y, z -> stack: a, b, c, x, y, z | locals: x, y, z
        AsmHelper.pushLocalToStack(validLocals, node, insnNodes);
        // x, y, z, a, b, c, JPortal, JContext
        List<Type> arguments = calcLoopArgumentsType(validLocals, node);
        MethodNode lambdaNode = createLambdaNode(arguments);
        insnNodes.add(LambdaUtils.invokeJAsyncPortalTask(
                methodContext.classType(),
                lambdaNode.name,
                methodContext.isStatic(),
                arguments.subList(0, arguments.size() - 2).toArray(new Type[arguments.size() - 2])
        ));
        insnNodes.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                Constants.JPROMISE_NAME,
                Constants.JPROMISE_PORTAL_NAME,
                Constants.JPROMISE_PORTAL_DESC.getDescriptor()
        ));
        insnNodes.add(new InsnNode(Opcodes.ARETURN));
        LabelNode portalLabel = new LabelNode();
        methodContext.replaceLabel(labelNode, portalLabel);
        int[] stacksHolder = new int[] {0};
        InsnList successors = collectSuccessors(node, (in, n) -> {
            if (n.getSuccessors().contains(node)) {
                stacksHolder[0] = Math.max(stacksHolder[0], n.getStackSize());
            }
            return methodContext.cloneInsn(in);
        });
        buildLambda(lambdaNode, arguments, successors, validLocals, node.getStackSize(), stacksHolder[0], portalLabel);
        return packageInsnNode;
    }

    private void buildLambda(MethodNode lambdaNode, List<Type> arguments, InsnList insnList, int locals, int stacks, int loopStacks, LabelNode portalLabel) {
        lambdaNode.visitCode();
        boolean isStatic = methodContext.isStatic();
        int offset = isStatic ? 0 : 1;
        // restore stack.
        // arguments: x, y, z, a, b, await type, context -> stack: a, b, await result | stack: a, b, c
        // if static, locals: x, y, z, a, b, await type, context | locals: x, y, z, a, b, c, portal, context
        // if not, locals: this, x, y, z, a, b, await type, context | locals: this, x, y, z, a, b, c, portal, context
        int iExtra1 = -1, iExtra2 = -1;
        Type tExtra1 = null, tExtra2 = null;
        AbstractInsnNode[] insnNodes = new AbstractInsnNode[stacks];
        int j = 0, k = 0;
        for (int i = 0; i < arguments.size(); ++i) {
            Type type = arguments.get(i);
            if (i >= locals && i < locals + stacks) {
                if (type != null) {
                    insnNodes[k++] = new VarInsnNode(type.getOpcode(Opcodes.ILOAD), j + offset);
                } else {
                    insnNodes[k++] = new InsnNode(Opcodes.ACONST_NULL);
                }
            } else if (i == locals + stacks) {
                iExtra1 = j;
                tExtra1 = type != null ? type : Constants.OBJECT_DESC;
            } else if (i == locals + stacks + 1) {
                iExtra2 = j;
                tExtra2 = type != null ? type : Constants.OBJECT_DESC;
            }
            if (type != null) {
                j += type.getSize();
            } else {
                ++j;
            }
        }
        if (iExtra1 < 0 || (portalLabel != null && iExtra2 < 0)) {
            throw new IllegalStateException("This is impossible.");
        }
        int maxStacks = methodContext.getMv().maxStack;
        int newMaxLocals = j + offset;
        int newMaxStacks = stacks;
        int maxLocals = methodContext.getMv().maxLocals;
        if (maxLocals > iExtra1) {
            lambdaNode.instructions.add(new VarInsnNode(tExtra1.getOpcode(Opcodes.ILOAD), iExtra1));
            lambdaNode.instructions.add(new VarInsnNode(tExtra1.getOpcode(Opcodes.ISTORE), maxLocals));
            iExtra1 = maxLocals;
            maxLocals += tExtra1.getSize();
            if (portalLabel != null) {
                lambdaNode.instructions.add(new VarInsnNode(tExtra2.getOpcode(Opcodes.ILOAD), iExtra2));
                lambdaNode.instructions.add(new VarInsnNode(tExtra2.getOpcode(Opcodes.ISTORE), maxLocals));
                maxLocals += tExtra2.getSize();
            }
            newMaxStacks = Math.max(newMaxLocals, 1);
        }
        for (AbstractInsnNode insnNode : insnNodes) {
            lambdaNode.instructions.add(insnNode);
        }
        lambdaNode.instructions.add(insnList);
        if (portalLabel != null) {
            lambdaNode.instructions.add(portalLabel);
            lambdaNode.instructions.add(new VarInsnNode(Opcodes.ALOAD, iExtra1));
            lambdaNode.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Constants.JPORTAL_NAME, Constants.JPORTAL_JUMP_NAME, Constants.JPORTAL_JUMP_DESC.getDescriptor()));
            lambdaNode.instructions.add(new InsnNode(Opcodes.ARETURN));
            newMaxStacks = Math.max(newMaxStacks, loopStacks + 1);
        }
        lambdaNode.visitMaxs(Math.max(maxStacks, newMaxStacks), Math.max(maxLocals, newMaxLocals));
        lambdaNode.visitEnd();

        MethodContext childContext = methodContext.createChild(lambdaNode);
        children.add(new CodePiece(childContext, this));
        methodContext.addLambdaContext(childContext);
    }
}
