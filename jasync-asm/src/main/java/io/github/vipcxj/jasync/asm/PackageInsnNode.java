package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PackageInsnNode extends AbstractInsnNode {

    private final List<AbstractInsnNode> insnNodes;

    protected PackageInsnNode() {
        super(-1);
        this.insnNodes = new ArrayList<>();
    }

    @Override
    public int getType() {
        return 0;
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
}
