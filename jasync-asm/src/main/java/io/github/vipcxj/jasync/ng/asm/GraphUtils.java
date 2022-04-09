package io.github.vipcxj.jasync.ng.asm;

import java.util.*;

public class GraphUtils {

    public static List<Set<Integer>> tarjan(Vertex[] nodes) {
        int visTime = 0;
        List<Set<Integer>> results = new ArrayList<>();
        int size = nodes.length;
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
            Vertex node = nodes[i];
            if (node != null) {
                assert node.getValue() == i;
                if (dfn[i] == 0) {
                    dfStack.clear();
                    itStack.clear();
                    dfStack.push(node);
                    itStack.push(node.createSuccessors());
                    while (!dfStack.isEmpty()) {
                        Vertex vertex = dfStack.peek();
                        int top = vertex.getValue();
                        Successors successors = itStack.element();
                        Vertex successor = successors.current();
                        if (dfn[top] == 0) {
                            sccStack.push(top);
                            vis[top] = true;
                            dfn[top] = low[top] = ++visTime;
                        }
                        if (successor != null) {
                            int index = successor.getValue();
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
                                    ssc.add(sscTop);
                                    vis[sscTop] = false;
                                }
                                ssc.add(sscTop);
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
}
