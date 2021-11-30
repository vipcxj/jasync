package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Value;

import java.util.*;

public class JAsyncValue extends BasicValue {

    private boolean uninitialized;
    private AbstractInsnNode newInsnNode;
    private Set<AbstractInsnNode> copyInsnNodes;


    /**
     * Constructs a new {@link BasicValue} of the given type.
     *
     * @param type the value type.
     */
    public JAsyncValue(Type type) {
        super(type);
        this.uninitialized = false;
    }

    public BasicValue merge(BasicValue other) {
        if (other == null) {
            return null;
        }
        if (this.equals(other)) {
            return this;
        }
        if (other instanceof JAsyncValue) {
            JAsyncValue asyncOther = (JAsyncValue) other;
            if (asyncOther.isUninitialized() != this.isUninitialized()) {
                return BasicValue.UNINITIALIZED_VALUE;
            }
        } else if (isUninitialized()) {
            return BasicValue.UNINITIALIZED_VALUE;
        }
        Type newType;
        if (this.getType() == null || other.getType() == null) {
            newType = null;
        } else {
            newType = AsmHelper.getNearestCommonAncestorType(this.getType(), other.getType());
        }
        if (newType == null) {
            return null;
        }
        JAsyncValue newValue = new JAsyncValue(newType);
        newValue.uninitialized = this.uninitialized;
        return newValue;
    }

    public static JAsyncValue newValue(Type type) {
        if (type != null && (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY)) {
            return new JAsyncValue(type);
        }
        return null;
    }

    public static BasicValue newOperation(AbstractInsnNode insn) throws AnalyzerException {
        if (insn.getOpcode() == Opcodes.NEW) {
            TypeInsnNode typeInsnNode = (TypeInsnNode) insn;
            JAsyncValue value = new JAsyncValue(Type.getObjectType(typeInsnNode.desc));
            value.setUninitialized(true);
            value.setNewInsnNode(insn);
            return value;
        } else {
            return null;
        }
    }

    public JAsyncValue copyOperation(AbstractInsnNode insn) {
        if (isUninitialized()) {
            if (copyInsnNodes == null) {
                copyInsnNodes = new HashSet<>();
            }
            copyInsnNodes.add(insn);
        }
        return this;
    }

    public static boolean isUninitialized(Value value) {
        if (value instanceof JAsyncValue) {
            JAsyncValue jAsyncValue = (JAsyncValue) value;
            return jAsyncValue.isUninitialized();
        } else {
            return false;
        }
    }

    public boolean isUninitialized() {
        return uninitialized;
    }

    public void setUninitialized(boolean uninitialized) {
        this.uninitialized = uninitialized;
    }

    public AbstractInsnNode getNewInsnNode() {
        return newInsnNode;
    }

    public void setNewInsnNode(AbstractInsnNode newInsnNode) {
        this.newInsnNode = newInsnNode;
    }

    public Set<AbstractInsnNode> getCopyInsnNodes() {
        return copyInsnNodes != null ? copyInsnNodes : Collections.emptySet();
    }

    @Override
    public String toString() {
        String s = super.toString();
        if (isUninitialized()) {
            return s + "(uninitialized)";
        } else {
            return s;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        JAsyncValue that = (JAsyncValue) o;
        return uninitialized == that.uninitialized;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), uninitialized);
    }
}
