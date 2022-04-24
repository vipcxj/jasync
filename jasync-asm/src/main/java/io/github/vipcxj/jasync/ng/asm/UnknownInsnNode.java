package io.github.vipcxj.jasync.ng.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.util.Map;

public class UnknownInsnNode extends AbstractInsnNode {

    protected UnknownInsnNode() {
        super(-1);
    }

    @Override
    public int getType() {
        return -1;
    }

    @Override
    public void accept(MethodVisitor methodVisitor) {
        if (methodVisitor instanceof TraceMethodVisitor) {
            TraceMethodVisitor visitor = (TraceMethodVisitor) methodVisitor;
            if (visitor.p instanceof InsnTextifier) {
                InsnTextifier textifier = (InsnTextifier) visitor.p;
                textifier.visitUnknownInsnNode();
            }
        }
    }

    @Override
    public AbstractInsnNode clone(Map<LabelNode, LabelNode> clonedLabels) {
        return new UnknownInsnNode();
    }
}
