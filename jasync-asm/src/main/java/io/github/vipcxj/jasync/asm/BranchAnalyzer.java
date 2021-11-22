package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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
                installLocalVars(nodes[i], methodNode, labelNodes);
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

    public static class Node<V extends Value> extends Frame<V> implements Vertex {

        private final Set< Node<? extends V> > successors = new HashSet<>();
        private int index;
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
                this.successors.addAll(other.successors);
                this.localVars = other.localVars.clone();
            } else {
                this.localVars = new LocalVar[frame.getLocals()];
            }
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
