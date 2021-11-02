package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.util.Textifier;

public class JAsyncPrinter extends Textifier {
    public JAsyncPrinter() {
        super(Constants.ASM_VERSION);
    }

    public void appendFrame(Frame<? extends BasicValue> frame) {
        stringBuilder.setLength(0);
        stringBuilder.append("FRAME: [");
        int locals = frame.getLocals();
        for (int i = 0; i < locals; ++i) {
            stringBuilder.append(frame.getLocal(i));
            if (i < locals - 1) {
                stringBuilder.append(", ");
            }
        }
        stringBuilder.append("][");
        int stackSize = frame.getStackSize();
        for (int i = 0; i < stackSize; ++i) {
            stringBuilder.append(frame.getStack(i));
            if (i < stackSize - 1) {
                stringBuilder.append(", ");
            }
        }
        stringBuilder.append("]\n");
        text.add(stringBuilder.toString());
    }

    public void visitInsn(AbstractInsnNode insnNode) {
        if (insnNode instanceof IntInsnNode) {
            visitIntInsn(insnNode.getOpcode(), ((IntInsnNode) insnNode).operand);
        } else if (insnNode instanceof VarInsnNode) {
            visitVarInsn(insnNode.getOpcode(), ((VarInsnNode) insnNode).var);
        } else if (insnNode instanceof TypeInsnNode) {
            visitTypeInsn(insnNode.getOpcode(), ((TypeInsnNode) insnNode).desc);
        } else if (insnNode instanceof FieldInsnNode) {
            FieldInsnNode fieldInsnNode = (FieldInsnNode) insnNode;
            visitFieldInsn(insnNode.getOpcode(), fieldInsnNode.owner, fieldInsnNode.name, fieldInsnNode.desc);
        } else if (insnNode instanceof MethodInsnNode) {
            MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
            visitMethodInsn(methodInsnNode.getOpcode(), methodInsnNode.owner, methodInsnNode.name, methodInsnNode.desc, methodInsnNode.itf);
        } else if (insnNode instanceof InvokeDynamicInsnNode) {
            InvokeDynamicInsnNode di = (InvokeDynamicInsnNode) insnNode;
            visitInvokeDynamicInsn(di.name, di.desc, di.bsm, di.bsmArgs);
        } else if (insnNode instanceof JumpInsnNode) {
            visitJumpInsn(insnNode.getOpcode(), ((JumpInsnNode) insnNode).label.getLabel());
        } else if (insnNode instanceof LabelNode) {
            visitLabel(((LabelNode) insnNode).getLabel());
        } else if (insnNode instanceof LdcInsnNode) {
            visitLdcInsn(((LdcInsnNode) insnNode).cst);
        } else if (insnNode instanceof IincInsnNode) {
            visitIincInsn(((IincInsnNode) insnNode).var, ((IincInsnNode) insnNode).incr);
        } else if (insnNode instanceof TableSwitchInsnNode) {
            TableSwitchInsnNode node = (TableSwitchInsnNode) insnNode;
            visitTableSwitchInsn(node.min, node.max, node.dflt.getLabel(), node.labels.stream().map(LabelNode::getLabel).toArray(Label[]::new));
        } else if (insnNode instanceof LookupSwitchInsnNode) {
            LookupSwitchInsnNode node = (LookupSwitchInsnNode) insnNode;
            visitLookupSwitchInsn(node.dflt.getLabel(), node.keys.stream().mapToInt(i -> i).toArray(), node.labels.stream().map(LabelNode::getLabel).toArray(Label[]::new));
        } else if (insnNode instanceof MultiANewArrayInsnNode) {
            visitMultiANewArrayInsn(((MultiANewArrayInsnNode) insnNode).desc, ((MultiANewArrayInsnNode) insnNode).dims);
        } else if (insnNode instanceof LineNumberNode) {
            visitLineNumber(((LineNumberNode) insnNode).line, ((LineNumberNode) insnNode).start.getLabel());
        } else if (insnNode.getOpcode() >= 0){
            visitInsn(insnNode.getOpcode());
        }
    }
}
