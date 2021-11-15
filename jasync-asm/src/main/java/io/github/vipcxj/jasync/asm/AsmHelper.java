package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Value;

import java.util.List;

public class AsmHelper {

    public static boolean isAwait(int opcode, String owner, String name, String desc) {
        if (Constants.AWAIT.equals(name)) {
            if (opcode == Opcodes.INVOKEINTERFACE || opcode == Opcodes.INVOKESPECIAL || opcode == Opcodes.INVOKEVIRTUAL) {
                if (Type.getArgumentTypes(desc).length == 0) {
                    return Utils.isJPromise(Type.getObjectType(owner).getClassName());
                }
            }
        }
        return false;
    }

    public static boolean isAwait(AbstractInsnNode insnNode) {
        if (insnNode instanceof MethodInsnNode) {
            MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
            return isAwait(methodInsnNode.getOpcode(), methodInsnNode.owner, methodInsnNode.name, methodInsnNode.desc);
        }
        return false;
    }

    public static int calcValidLocals(Frame<?> frame) {
        int locals = frame.getLocals();
        int valid = -1;
        for (int i = 0; i < locals; ++i) {
            Value value = frame.getLocal(i);
            if (value == null) {
                if (valid == -1) {
                    valid = i;
                }
            } else {
                if (valid != -1) {
                    valid = -1;
                }
            }
        }
        return valid == -1 ? locals : valid;
    }

    public static int storeStackToLocal(int validLocals, Frame<? extends BasicValue> frame, List<AbstractInsnNode> results) {
        int iLocal = validLocals;
        int stackSize = frame.getStackSize();
        for (int i = stackSize - 1; i >= 0; --i) {
            BasicValue value = frame.getStack(i);
            Type type = value.getType();
            if (type != null) {
                results.add(new VarInsnNode(type.getOpcode(Opcodes.ISTORE), iLocal));
                iLocal += type.getSize();
            } else {
                results.add(new VarInsnNode(Opcodes.ASTORE, iLocal));
                iLocal += 1;
            }
        }
        return iLocal;
    }

    public static void pushLocalToStack(int validLocals, Frame<? extends BasicValue> frame, List<AbstractInsnNode> results) {
        for (int i = 0; i < validLocals;) {
            BasicValue value = frame.getLocal(i);
            if (value != null && value.getType() != null) {
                Type type = value.getType();
                results.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), i));
                i += type.getSize();
            } else {
                results.add(new InsnNode(Opcodes.ACONST_NULL));
                ++i;
            }
        }
    }
}
