package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.util.*;

public class BranchAnalyzer extends Analyzer<BasicValue> {

    private Node<BasicValue>[] nodes;
    private MethodNode methodNode;

    /**
     * Constructs a new {@link Analyzer}.
     *
     */
    public BranchAnalyzer() {
        super(new TypeInterpreter());
    }

    @Override
    public Frame<BasicValue>[] analyze(String owner, MethodNode method) throws AnalyzerException {
        this.methodNode = method;
        return super.analyze(owner, method);
    }

    @Override
    protected Frame<BasicValue> newFrame(Frame<? extends BasicValue> frame) {
        return new Node<>(frame);
    }

    @Override
    protected Frame<BasicValue> newFrame(int numLocals, int numStack) {
        return new Node<>(numLocals, numStack);
    }

    @Override
    protected void newControlFlowEdge(int insnIndex, int successorIndex) {
        Node<BasicValue> frame = (Node<BasicValue>) getFrames()[insnIndex];
        Node<BasicValue> successor = (Node<BasicValue>) getFrames()[successorIndex];
        frame.successors.add(successor);
        successor.precursors.add(frame);
    }

    public Node<BasicValue>[] getNodes() {
        if (nodes != null) {
            return nodes;
        }
        if (methodNode == null) {
            throw new IllegalStateException("Call analyze first.");
        }
        Frame<BasicValue>[] frames = getFrames();
        //noinspection unchecked
        nodes = new Node[frames.length];
        Set<LabelNode> labelNodes = new HashSet<>();
        for (int i = 0; i < frames.length; ++i) {
            AbstractInsnNode insnNode = methodNode.instructions.get(i);
            if (insnNode instanceof LabelNode) {
                labelNodes.add((LabelNode) insnNode);
            }
            nodes[i] = (Node<BasicValue>) frames[i];
            if (nodes[i] != null) {
                nodes[i].index = i;
                nodes[i].insnNode = insnNode;
                installLocalVars(nodes[i], methodNode, labelNodes);
            }
        }
        for (Node<BasicValue> node : nodes) {
            if (node != null) {
                checkCast(node);
            }
        }
        return nodes;
    }

    private void installLocalVars(Node<BasicValue> node, MethodNode methodNode, Set<LabelNode> visitedLabels) {
        if (methodNode.localVariables != null) {
            for (LocalVariableNode localVariable : methodNode.localVariables) {
                if (visitedLabels.contains(localVariable.start) && !visitedLabels.contains(localVariable.end)) {
                    node.localVars[localVariable.index] = new LocalVar(localVariable.name, localVariable.desc, localVariable.signature);
                }
            }
        }
    }

    private void checkCast(Node<? extends BasicValue> node) {
        if (node.insnNode instanceof MethodInsnNode) {
            MethodInsnNode methodInsnNode = (MethodInsnNode) node.insnNode;
            Type[] argumentTypes = Type.getArgumentTypes(methodInsnNode.desc);
            if (methodInsnNode.getOpcode() != Opcodes.INVOKESTATIC) {
                Type[] newArgumentTypes = new Type[argumentTypes.length + 1];
                newArgumentTypes[0] = Type.getObjectType(methodInsnNode.owner);
                System.arraycopy(argumentTypes, 0, newArgumentTypes, 1, argumentTypes.length);
                argumentTypes = newArgumentTypes;
            }
            int stackSize = node.getStackSize();
            List<Node<? extends BasicValue>> entries = new ArrayList<>();
            entries.add(node);
            for (int i = 0; i < argumentTypes.length; ++ i) {
                int stack = stackSize - 1 - i;
                if (stack < 0) {
                    throw new IllegalStateException("The stack broken.");
                }
                Type argType = argumentTypes[i];
                List<Node<? extends BasicValue>> newEntries = new ArrayList<>();
                for (Node<? extends BasicValue> entry : entries) {
                    newEntries.addAll(flagCast(entry, argType, stack));
                }
                entries = newEntries;
            }
        }
    }

    private List<Node<? extends BasicValue>> flagCast(Node<? extends BasicValue> node, Type type, int stack) {
        List<Node<? extends BasicValue>> results = new ArrayList<>();
        int stackSize = node.getStackSize();
        if (stackSize - 1 == stack) {
            BasicValue value = node.getStack(stack);
            if (value == null) {
                throw new NullPointerException();
            }
            if (value.getType() != null && value.getType().equals(Constants.OBJECT_DESC)) {
                node.needCastTo = type;
            }
            results.add(node);
        } else if (stackSize - 1 > stack) {
            for (Node<? extends BasicValue> precursor : node.getPrecursors()) {
                int nextStack = stack;
                BasicValue value1, value2, value3, value4;
                boolean form1;
                switch (precursor.insnNode.getOpcode()) {
                    case DUP_X1: // a b a <- b a
                        // a [b] a <- [b] a
                        if (stack == stackSize - 2) {
                            nextStack = stack - 1;
                        }
                        // [a] b a <- b [a]
                        else if (stack == stackSize - 3) {
                            nextStack = stack + 1;
                        }
                        break;
                    case DUP_X2: // a c b a <- c b a
                        // a c [b] a <- c [b] a || a [c] b a <- [c] b a
                        if (stack == stackSize - 2 || stack == stackSize - 3) {
                            nextStack = stack - 1;
                        }
                        // [a] c b a <- c b [a]
                        else if (stack == stackSize - 4) {
                            nextStack = stack + 2;
                        }
                        break;
                    case DUP2: // (b a) (b a) <- (b a)
                        value1 = node.getStack(stackSize - 1);
                        value2 = node.getStack(stackSize - 2);
                        form1 = (value1.getType() == null || value1.getType().getSize() == 1)
                                && (value2.getType() == null || value2.getType().getSize() == 1);
                        // (b a) ([b] a) <- ([b] a) || [a] a <- [a]
                        if (stack == stackSize - 2 && form1) {
                            nextStack = stack - 2;
                        }
                        // (b [a]) (b a) <- (b [a]) || [?] a a <- [?] a || ([b] a) (b a) <- ([b] a) || [?] ? a a <- [?] ? a
                        break;
                    case DUP2_X1: // (b a) c (b a) <- c (b a)
                        value1 = node.getStack(stackSize - 1);
                        value2 = node.getStack(stackSize - 2);
                        value3 = node.getStack(stackSize - 3);
                        form1 = (value1.getType() == null || value1.getType().getSize() == 1)
                                && (value2.getType() == null || value2.getType().getSize() == 1)
                                && (value3.getType() == null || value3.getType().getSize() == 1);
                        if (stack == stackSize - 2) {
                            // (b a) c ([b] a) <- c ([b] a)
                            if (form1) {
                                nextStack = stack - 2;
                            }
                            // a [b] a <- [b] a
                            else {
                                nextStack = stack - 1;
                            }
                        } else if (stack == stackSize - 3) {
                            // (b a) [c] (b a) <- [c] (b a)
                            if (form1) {
                                nextStack = stack - 2;
                            }
                            // [a] b a <- b [a]
                            else {
                                nextStack = stack + 1;
                            }
                        }
                        // (b [a]) c (b a) <- c (b [a]) || ([b] a) c (b a) <- c ([b] a)
                        else if ((stack == stackSize - 4 || stack == stackSize - 5) && form1) {
                            nextStack = stack + 1;
                        }
                        break;
                    case DUP2_X2: // (v2 v1) (v4 v3) (v2 v1) <- (v4 v3) (v2 v1)
                        value1 = node.getStack(stackSize - 1);
                        value2 = node.getStack(stackSize - 2);
                        value3 = node.getStack(stackSize - 3);
                        value4 = node.getStack(stackSize - 4);
                        form1 = (value1.getType() == null || value1.getType().getSize() == 1)
                                && (value2.getType() == null || value2.getType().getSize() == 1)
                                && (value3.getType() == null || value3.getType().getSize() == 1)
                                && (value4.getType() == null || value4.getType().getSize() == 1);
                        if (stack == stackSize - 2 || stack == stackSize - 3) {
                            // (v2 v1) (v4 v3) ([v2] v1) <- (v4 v3) ([v2] v1)
                            // (v2 v1) (v4 [v3]) (v2 v1) <- (v4 [v3]) (v2 v1)
                            if (form1) {
                                nextStack = stack - 2;
                            }
                            // v1 v3 [v2] v1 <- v3 [v2] v1
                            // v1 [v3] v2 v1 <- [v3] v2 v1
                            else {
                                nextStack = stack - 1;
                            }
                        } else if (stack == stackSize - 4) {
                            // (v2 v1) ([v4] v3) (v2 v1) <- ([v4] v3) (v2 v1)
                            if (form1) {
                                nextStack = stack - 2;
                            }
                            // [v1] v3 v2 v1 <- v3 v2 [v1]
                            else {
                                nextStack = stack + 2;
                            }
                        }
                        // (v2 [v1]) (v4 v3) (v2 v1) <- (v4 v3) (v2 [v1])
                        // ([v2] v1) (v4 v3) (v2 v1) <- (v4 v3) ([v2] v1)
                        else if ((stack == stackSize - 5 || stack == stackSize - 6) && form1) {
                            nextStack = stack + 2;
                        }
                        break;
                    case SWAP: // a b <- b a
                        // [a] b <- b [a]
                        if (stack == stackSize - 2) {
                            nextStack = stack + 1;
                        }
                        break;
                    default:
                        nextStack = stack;
                }
                results.addAll(flagCast(precursor, type, nextStack));
            }
        } else {
            throw new IllegalStateException("This is impossible! Unknown insn: " + node.insnNode.getOpcode() + ".");
        }
        return results;
    }

    public static class Node<V extends Value> extends Frame<V> implements Vertex {

        private final Set<Node<? extends V>> precursors = new HashSet<>();
        private final Set< Node<? extends V> > successors = new HashSet<>();
        private int index;
        private AbstractInsnNode insnNode;
        private Type needCastTo;
        private final LocalVar[] localVars;

        public Node(int numLocals, int maxStack) {
            super(numLocals, maxStack);
            this.localVars = new LocalVar[numLocals];
        }

        public Node(Frame<? extends V> frame) {
            super(frame);
            if (frame instanceof Node) {
                //noinspection unchecked
                Node<? extends V> other = (Node<? extends V>) frame;
                this.index = other.index;
                this.precursors.addAll(other.precursors);
                this.successors.addAll(other.successors);
                this.localVars = other.localVars.clone();
            } else {
                this.localVars = new LocalVar[frame.getLocals()];
            }
        }

        public Set<Node<? extends V>> getPrecursors() {
            return precursors;
        }

        @Override
        public SuccessorsImpl createSuccessors() {
            return new SuccessorsImpl(successors.iterator());
        }

        public Set<Node<? extends V>> getSuccessors() {
            return successors;
        }

        public LocalVar[] getLocalVars() {
            return localVars;
        }

        public AbstractInsnNode getInsnNode() {
            return insnNode;
        }

        public Type getNeedCastTo() {
            return needCastTo;
        }

        @Override
        public int getValue() {
            return index;
        }

        public int getIndex() {
            return index;
        }

        public class SuccessorsImpl implements Successors {

            private final Iterator<Node<? extends V>> iterator;
            private Node<? extends V> current;

            SuccessorsImpl(Iterator<Node<? extends V>> iterator) {
                this.iterator = iterator;
                this.current = iterator.hasNext() ? iterator.next() : null;
            }

            @Override
            public void next() {
                this.current = iterator.hasNext() ? iterator.next() : null;
            }

            @Override
            public Node<? extends V> current() {
                return current;
            }
        }
    }

}
