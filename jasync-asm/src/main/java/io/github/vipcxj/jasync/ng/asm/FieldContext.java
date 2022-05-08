package io.github.vipcxj.jasync.ng.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FieldContext {
    private final int access;
    private final String name;
    private final String descriptor;
    private final String signature;
    private final Object value;
    private List<AbstractInsnNode> initInsnNodes;
    private int maxStack;
    private int maxLocal;

    public FieldContext(int access, String name, String descriptor, String signature, Object value) {
        this.access = access;
        this.name = name;
        this.descriptor = descriptor;
        this.signature = signature;
        this.value = value;
        this.initInsnNodes = null;
        this.maxStack = 0;
        this.maxLocal = 0;
    }

    public static FieldContext createPrivateStaticFinalField(String name, String typeDesc) {
        return new FieldContext(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC, name, typeDesc, null, null);
    }

    public void addInitInsnNode(AbstractInsnNode insnNode) {
        if (initInsnNodes == null) {
            initInsnNodes = new ArrayList<>();
        }
        initInsnNodes.add(insnNode);
    }

    public int getAccess() {
        return access;
    }

    public String getName() {
        return name;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public String getSignature() {
        return signature;
    }

    public Object getValue() {
        return value;
    }

    public void updateMaxStack(int maxStack) {
        this.maxStack = maxStack;
    }

    public void updateMaxLocal(int maxLocal) {
        this.maxLocal = maxLocal;
    }

    public List<AbstractInsnNode> getInitInsnNodes() {
        return initInsnNodes != null ? initInsnNodes : Collections.emptyList();
    }

    public void accept(ClassVisitor visitor) {
        visitor.visitField(
                getAccess(),
                getName(),
                getDescriptor(),
                getSignature(),
                getValue()
        );
        if (initInsnNodes != null) {
            MethodVisitor methodVisitor = visitor.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            methodVisitor.visitCode();
            for (AbstractInsnNode insnNode : initInsnNodes) {
                insnNode.accept(methodVisitor);
            }
            methodVisitor.visitMaxs(maxStack, maxLocal);
            methodVisitor.visitEnd();
        }
    }
}
