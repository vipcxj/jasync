package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.tree.AbstractInsnNode;

public class InsnWithIndex {
    private final AbstractInsnNode insnNode;
    private final int index;

    public InsnWithIndex(AbstractInsnNode insnNode, int index) {
        this.insnNode = insnNode;
        this.index = index;
    }

    public AbstractInsnNode getInsnNode() {
        return insnNode;
    }

    public int getIndex() {
        return index;
    }
}
