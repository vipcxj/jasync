package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.*;

public abstract class CodePiece {
    protected final MethodContext methodContext;
    protected final CodePiece parent;
    protected final int from;
    protected final int to;
    protected final List<InsnWithIndex> insnNodes;

    protected CodePiece(MethodContext methodContext, CodePiece parent, int from, int to) {
        this.methodContext = methodContext;
        this.parent = parent;
        assert to > from;
        this.from = from;
        this.to = to;
        this.insnNodes = new ArrayList<>(to - from);
        collectInsnNodes();
    }

    private void collectInsnNodes() {
        InsnList instructions = methodContext.getMv().instructions;
        int i = from;
        AbstractInsnNode node = instructions.get(from);
        AbstractInsnNode toNode = to < instructions.size() ? instructions.get(to) : null;
        do {
            insnNodes.add(new InsnWithIndex(node, i++));
            node = node.getNext();
        }
        while (node != toNode);
    }

    private boolean isAwait(AbstractInsnNode node) {
        if (node.getType() == AbstractInsnNode.METHOD_INSN && node.getOpcode() != Opcodes.INVOKESTATIC) {
            MethodInsnNode methodInsnNode = (MethodInsnNode) node;
            if (Constants.AWAIT.equals(methodInsnNode.name)) {
                Type[] argumentTypes = Type.getArgumentTypes(methodInsnNode.desc);
                if (argumentTypes.length == 0) {
                    Type objectType = Type.getObjectType(methodInsnNode.owner);
                    return Utils.isJPromise(objectType.getClassName());
                }
            }
        }
        return false;
    }

    public boolean inRange(int index) {
        return index >= from && index < to;
    }

    public void prepare(int input) {
    }

    public static List<Set<Integer>> tarjan(Vertex[] nodes, int from, int to) {
        int visTime = 0;
        List<Set<Integer>> results = new ArrayList<>();
        int size = to - from;
        int[] dfn = new int[size];
        int[] low = new int[size];
        boolean[] vis = new boolean[size];
        Arrays.fill(dfn, 0);
        Arrays.fill(low, 0);
        Arrays.fill(vis, false);
        ArrayDeque<Integer> sccStack = new ArrayDeque<>();
        ArrayDeque<Vertex> dfStack = new ArrayDeque<>();
        ArrayDeque<Successors> itStack = new ArrayDeque<>();
        for (int i = 0; i < size; ++i) {
            Vertex node = nodes[i + from];
            if (node != null) {
                assert node.getValue() == i + from;
                if (dfn[i] == 0) {
                    dfStack.clear();
                    itStack.clear();
                    dfStack.push(node);
                    itStack.push(node.createSuccessors());
                    while (!dfStack.isEmpty()) {
                        Vertex vertex = dfStack.peek();
                        int top = vertex.getValue() - from;
                        Successors successors = itStack.element();
                        Vertex successor = successors.current();
                        if (dfn[top] == 0) {
                            sccStack.push(top);
                            vis[top] = true;
                            dfn[top] = low[top] = ++visTime;
                        }
                        if (successor != null) {
                            int index = successor.getValue() - from;
                            if (dfn[index] == 0) {
                                dfStack.push(successor);
                                itStack.push(successor.createSuccessors());
                            } else if (dfn[index] < 0) {
                                dfn[index] = -dfn[index];
                                low[top] = Math.min(low[top], low[index]);
                                successors.next();
                            } else {
                                if (vis[index]) {
                                    low[top] = Math.min(low[top], dfn[index]);
                                }
                                successors.next();
                            }
                        } else {
                            if (dfn[top] == low[top] || dfn[top] == -low[top]) {
                                Set<Integer> ssc = new HashSet<>();
                                int sscTop;
                                while ((sscTop = sccStack.pop()) != top) {
                                    ssc.add(sscTop + from);
                                    vis[sscTop] = false;
                                }
                                ssc.add(sscTop + from);
                                vis[sscTop] = false;
                                results.add(ssc);
                            }
                            if (dfStack.size() > 1) {
                                dfn[top] = -dfn[top];
                            }
                            dfStack.pop();
                            itStack.pop();
                        }
                    }
                }
            }
        }
        return results;
    }

    public abstract InsnList transform();
}
