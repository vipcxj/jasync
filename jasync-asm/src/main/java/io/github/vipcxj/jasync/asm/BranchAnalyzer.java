package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.*;

import java.util.*;

public class BranchAnalyzer extends Analyzer<BasicValue> {

    private MethodNode method;
    private Node<BasicValue>[] nodes;

    /**
     * Constructs a new {@link Analyzer}.
     *
     */
    public BranchAnalyzer() {
        super(new TypeInterpreter());
    }

    @Override
    public Frame<BasicValue>[] analyze(String owner, MethodNode method) throws AnalyzerException {
        this.method = method;
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
        frame.successors.add((Node<BasicValue>) getFrames()[successorIndex]);
    }

    public Node<BasicValue>[] getNodes() {
        if (nodes != null) {
            return nodes;
        }
        Frame<BasicValue>[] frames = getFrames();
        //noinspection unchecked
        Node<BasicValue>[] nodes = new Node[frames.length];
        for (int i = 0; i < frames.length; ++i) {
            nodes[i] = (Node<BasicValue>) frames[i];
            nodes[i].index = i;
        }
        return nodes;
    }

    public void normalize() {
        Frame<BasicValue>[] frames = getFrames();
        for (int i = 0; i < frames.length; ++i) {
            Node<BasicValue> frame = (Node<BasicValue>) frames[i];
            if (frame != null) {
                frame.setIndex(i);
                boolean lineSet = false;
                for (int j = i; j >= 0; --j) {
                    AbstractInsnNode insnNode = method.instructions.get(j);
                    if (insnNode instanceof LabelNode) {
                        frame.setLabel(((LabelNode) insnNode).getLabel());
                        break;
                    } else if (insnNode instanceof LineNumberNode && !lineSet) {
                        frame.setLine(((LineNumberNode) insnNode).line);
                        lineSet = true;
                    }
                }
            }
        }
    }

    public void printInst() {
        Frame<BasicValue>[] frames = getFrames();
        JAsyncPrinter printer = new JAsyncPrinter();
        int i = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            Frame<BasicValue> frame = frames[i];
            if (frame != null) {
                printer.appendFrame(frame);
            }
            printer.visitInsn(instruction);
            ++i;
        }
        for (Object o : printer.getText()) {
            System.out.print(o);
        }
        System.out.println();
    }

    public void printFrameType() {
        Frame<BasicValue>[] frames = getFrames();
        int j = 0;
        for (Frame<BasicValue> frame : frames) {
            System.out.println(method.instructions.get(j));
            if (frame != null) {
                int locals = frame.getLocals();
                if (locals > 0) {
                    System.out.println("Locals: ");
                    for (int i = 0; i < locals; ++i) {
                        BasicValue local = frame.getLocal(i);
                        if (i < locals - 1) {
                            System.out.print(local + ", ");
                        } else {
                            System.out.println(local + ".");
                        }
                    }
                }
                int stackSize = frame.getStackSize();
                if (stackSize > 0) {
                    System.out.println("Stacks: ");
                    for (int i = 0; i < stackSize;) {
                        BasicValue stack = frame.getStack(i);
                        if (i < stackSize - 1) {
                            System.out.print(stack + ", ");
                        } else {
                            System.out.println(stack + ".");
                        }
                        int offset = stack.getSize();
                        i += offset;
                    }
                }
            }
            ++j;
        }
    }

    public Node<? extends BasicValue> simplify(Node<? extends BasicValue> root, Set<Node<? extends BasicValue>> pool) {
        if (pool.contains(root)) {
            return root;
        }
        pool.add(root);
        Node<BasicValue> newRoot = new Node<>(root);
        Set<Node<? extends BasicValue>> newSuccessors = new HashSet<>(newRoot.successors);
        for (Node<? extends BasicValue> successor : newRoot.successors) {
            int index = newRoot.index;
            while (successor.index - index == 1 && successor.successors.size() == 1) {
                index = successor.index;
                newSuccessors.remove(successor);
                successor = successor.successors.iterator().next();
                newSuccessors.add(successor);
            }
        }
        newRoot.successors.clear();
        for (Node<? extends BasicValue> successor : newSuccessors) {
            newRoot.successors.add(simplify(successor, pool));
        }
        return newRoot;
    }

    public void print() {
        Frame<BasicValue>[] frames = getFrames();
        if (frames.length > 0) {
            Node<? extends BasicValue> root = (Node<? extends BasicValue>) frames[0];
            Node<? extends BasicValue> newRoot = simplify(root, new HashSet<>());
            print(newRoot);
        }
    }

    public static void print(Node<? extends BasicValue> root) {
        List<String> lines = print(root, new HashSet<>());
        for (String line : lines) {
            System.out.println(line);
        }
    }

    private static List<String> print(Node<? extends BasicValue> node, Set<Node<? extends BasicValue>> pool) {
        pool.add(node);
        List<String> results = new ArrayList<>();
        int childNum = node.successors.size();
        int i = 0;
        for (Node<? extends BasicValue> successor : node.successors) {
            List<String> lines;
            if (pool.contains(successor)) {
                lines = Collections.singletonList(printNode(successor));
            } else {
                lines = print(successor, pool);
            }
            int j = 0;
            for (String line : lines) {
                if (j == 0) {
                    if (i == 0) {
                        results.add(" ----- " + line);
                    } else {
                        results.add("   |-- " + line);
                    }
                } else {
                    if (i < childNum - 1) {
                        results.add("   |   " + line);
                    } else {
                        results.add("       " + line);
                    }
                }
                ++j;
            }
            ++i;
        }
        if (results.isEmpty()) {
            results.add("");
        }
        String self = printNode(node);
        for (int j = 0; j < results.size(); ++j) {
            String line = results.get(j);
            if (j == 0) {
                line = self + line;
            } else {
                line = space(self.length()) + line;
            }
            results.set(j, line);
        }
        return results;
    }

    private static String space(int size) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; ++i) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static String printNode(Node<? extends BasicValue> node) {
        if (node.line != -1) {
            return "line:" + node.line;
        } else if (node.label != null){
            return "label:" + node.label;
        } else {
            return "inst:" + node.index;
        }
    }

    public static class Node<V extends Value> extends Frame<V> implements Vertex {

        private final Set< Node<? extends V> > successors = new HashSet<>();
        private int index;
        private int line = -1;
        private Label label;

        public Node(int numLocals, int maxStack) {
            super(numLocals, maxStack);
        }

        public Node(Frame<? extends V> frame) {
            super(frame);
            if (frame instanceof Node) {
                //noinspection unchecked
                Node<? extends V> other = (Node<? extends V>) frame;
                this.index = other.index;
                this.line = other.line;
                this.label = other.label;
                this.successors.addAll(other.successors);
            }
        }

        @Override
        public Successors createSuccessors() {
            return new SuccessorsImpl(successors.iterator());
        }

        @Override
        public int getValue() {
            return index;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public int getLine() {
            return line;
        }

        public void setLine(int line) {
            this.line = line;
        }

        public Label getLabel() {
            return label;
        }

        public void setLabel(Label label) {
            this.label = label;
        }

        public class SuccessorsImpl implements Successors {

            private final Iterator<Node<? extends V>> iterator;
            private Node<?> current;

            SuccessorsImpl(Iterator<Node<? extends V>> iterator) {
                this.iterator = iterator;
                this.current = iterator.hasNext() ? iterator.next() : null;
            }

            @Override
            public void next() {
                this.current = iterator.hasNext() ? iterator.next() : null;
            }

            @Override
            public Vertex current() {
                return current;
            }
        }
    }

}
