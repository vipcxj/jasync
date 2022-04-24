package io.github.vipcxj.jasync.ng.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PackageInsnNode extends AbstractInsnNode {

    private final List<AbstractInsnNode> insnNodes;
    private LabelNode endNode;

    protected PackageInsnNode() {
        super(-1);
        this.insnNodes = new ArrayList<>();
    }

    @Override
    public int getType() {
        return -1;
    }

    @Override
    public void accept(MethodVisitor methodVisitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AbstractInsnNode clone(Map<LabelNode, LabelNode> clonedLabels) {
        throw new UnsupportedOperationException();
    }

    public List<AbstractInsnNode> getInsnNodes() {
        return insnNodes;
    }

    public LabelNode getEndNode() {
        return endNode;
    }

    public void complete() {
        endNode = new LabelNode();
        insnNodes.add(endNode);
    }
}
